(ns vizard.server
  (:require
   [clojure.string :as str]
   [ring.middleware.defaults]
   [ring.util.response :refer (resource-response content-type)]
   [compojure.core :as comp :refer (defroutes GET POST)]
   [compojure.route :as route]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.encore :as encore]
   [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente  :as sente]
   [aleph.http :as aleph]
   [taoensso.sente.server-adapters.aleph :refer (get-sch-adapter)]
   [taoensso.sente.packers.transit :as sente-transit]
   [cheshire.core :as json]
   [clojure.java.io :as io]))

(timbre/set-level! :info)
(reset! sente/debug-mode?_ true)

(let [packer (sente-transit/get-transit-packer)
      chsk-server (sente/make-channel-socket-server! (get-sch-adapter) {:packer packer})
      {:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]} chsk-server]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(add-watch connected-uids :connected-uids
           (fn [_ _ old new]
             (when (not= old new)
               (infof "Connected uids change: %s" new))))

(def current-vl-spec (atom {}))
(def current-spec (atom {}))

(defn unique-id
  "Get a unique id for a session."
  []
  (str (java.util.UUID/randomUUID)))

(defn session-uid
  "Get session uuid from a request."
  [req]
  (get-in req [:session :uid]))

(defroutes my-routes
  (GET  "/" req (content-type {:status 200
                               :session (if (session-uid req)
                                          (:session req)
                                          (assoc (:session req) :uid (unique-id)))
                               :body (io/input-stream (io/resource "public/index.html"))} "text/html"))
  (POST "/spec" req
        (debugf "/spec got: %s" req)
        (let [spec (json/parse-string (slurp (:body req)))]
          (reset! current-spec)
          (doseq [uid (:any @connected-uids)]
            (chsk-send! uid [:vizard/spec spec]))
          {:status 200}))
  (POST "/vl-spec" req
        (debugf "/vl-spec got: %s" req)
        (let [vl-spec (json/parse-string (slurp (:body req)))]
          (reset! current-vl-spec vl-spec)
          (doseq [uid (:any @connected-uids)]
            (chsk-send! uid [:vizard/vl-spec vl-spec]))
          {:status 200}))
  (GET  "/chsk" req
        (debugf "/chsk got: %s" req)
        (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/resources "/")
  (route/not-found "<h1>Nope</h1>"))

(def main-ring-handler
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
                  false
                  #_{:read-token (fn [req] (-> req :params :csrf-token))})]
    (ring.middleware.defaults/wrap-defaults my-routes ring-defaults-config)))

(defmulti -event-msg-handler :id)

(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (tracef "Event: %s" event)
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid (:uid session)]
    (tracef "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk event-msg-handler)))

(defonce web-server_ (atom nil))
(defn stop-web-server! [] (when-let [stop-fn @web-server_] (stop-fn)))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [port (or port 0) ; 0 => Choose any available port
        ring-handler (var main-ring-handler)
        [port stop-fn]
        (let [server (aleph/start-server ring-handler {:port port})
              p (promise)]
          (future @p) ; Workaround for Ref. https://goo.gl/kLvced
          ;; (aleph.netty/wait-for-close server)
          [(aleph.netty/port server)
           (fn [] (.close ^java.io.Closeable server) (deliver p nil))])
        uri (format "http://localhost:%s/" port)]
    (infof "Web server is running at `%s`" uri)
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))

    (reset! web-server_ stop-fn)))

(defn stop! []
  (stop-router!)
  (stop-web-server!))

(defn start!
  ([]
   (start-router!)
   (start-web-server! 10666))
  ([& [port]]
   (start-router!)
   (start-web-server! port)))

(defn -main []
  (start!))
