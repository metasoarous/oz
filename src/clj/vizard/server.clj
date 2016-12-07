(ns vizard.server
  (:require
   [clojure.string :as str]
   [ring.middleware.defaults]
   [ring.middleware.anti-forgery :refer (*anti-forgery-token*)]
   [ring.util.response :refer (resource-response content-type)]
   [compojure.core :as comp :refer (defroutes GET POST)]
   [compojure.route :as route]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.encore :as encore :refer (have have?)]
   [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente  :as sente]
   [aleph.http :as aleph]
   [taoensso.sente.server-adapters.aleph :refer (get-sch-adapter)]
   [taoensso.sente.packers.transit :as sente-transit]
   [cheshire.core :as json]
   [vizard.schema :refer [Vega]]
   [schema.core :as s]
   [clojure.java.io :as io])
  (:gen-class))

(timbre/set-level! :info)
;; (reset! sente/debug-mode?_ true)

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

(def last-vl-spec (atom {}))
(def last-spec (atom {}))

(add-watch last-spec :last-spec
           (fn [_ _ old new]
             (when (not= old new)
               (debugf "last spec change: %s" new))))

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
  (GET "/token" req (json/generate-string {:csrf-token *anti-forgery-token*}))
  (POST "/spec" req
        (debugf "POST /spec got: %s" req)
        (let [spec (json/parse-string (slurp (:body req)))]
          (reset! last-spec spec)
          (doseq [uid (:any @connected-uids)]
            (chsk-send! uid [:vizard/spec spec]))
          {:status 200}))
  (GET "/spec" req
       (debugf "GET /spec got: %s" req)
       {:status 200
        :content-type "application/json"
        :body (json/generate-string @last-spec)})
  (POST "/vl-spec" req
        (debugf "POST /vl-spec got: %s" req)
        (let [vl-spec (json/parse-string (slurp (:body req)) true)]
          (reset! last-vl-spec vl-spec)
          (doseq [uid (:any @connected-uids)]
            (chsk-send! uid [:vizard/vl-spec vl-spec]))
          {:status 200}))
  (GET "/vl-spec" req
       (debugf "GET /vl-spec got: %s" req)
       {:status 200
        :content-type "application/json"
        :body (json/generate-string @last-vl-spec)})
  (GET  "/chsk" req
        (debugf "/chsk got: %s" req)
        (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/resources "/")
  (route/not-found "<h1>Nope</h1>"))

(def main-ring-handler
  (ring.middleware.defaults/wrap-defaults my-routes ring.middleware.defaults/site-defaults))

(defmulti -event-msg-handler :id)

(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (tracef "Event: %s" event)
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :vizard/to-vega
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (reset! last-spec ?data))

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

(defn -main [& [port]]
  (if port
    (start! (Integer/parseInt port))
    (start!)))
