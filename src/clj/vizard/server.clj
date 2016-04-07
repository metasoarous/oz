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
   [org.httpkit.server :as http-kit]
   [org.httpkit.client :as client]
   [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
   [taoensso.sente.packers.transit :as sente-transit]
   [clojure.java.io :as io]
   [cheshire.core :as json]))

(defn start-web-server!* [ring-handler port]
  (println "Starting http-kit...")
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server nil
     :port (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(def packer (sente-transit/get-flexi-packer :edn))

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {:packer packer})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

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
        (doseq [uid (:any @connected-uids)]
          (chsk-send! uid [:vizard/spec (json/parse-string (slurp (:body req)))]))
        {:status 200})
  (POST "/vl-spec" req
        (doseq [uid (:any @connected-uids)]
          (chsk-send! uid [:vizard/vl-spec (json/parse-string (slurp (:body req)))]))
        {:status 200})
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/resources "/")
  (route/not-found "<h1>Nope</h1>"))

(def my-ring-handler
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
                  false
                  #_{:read-token (fn [req] (-> req :params :csrf-token))})]
    (ring.middleware.defaults/wrap-defaults my-routes ring-defaults-config)))

(defmulti event-msg-handler :id)

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (tracef "Event: %s" event)
  (event-msg-handler ev-msg))

(do
  (defmethod event-msg-handler :default
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid (:uid session)]
      (tracef "Unhandled event: %s" event)
      (when ?reply-fn
        (?reply-fn {:umatched-event-as-echoed-from-from-server event})))))

(defonce web-server_ (atom nil))

(defn stop-web-server! []
  (when-let [m @web-server_] ((:stop-fn m))))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map}
        (start-web-server!* (var my-ring-handler)
                            (or port 0) ; 0 => auto (any available) port
                            )
        uri (format "http://localhost:%s/" port)]
    (debugf "Web server is running at `%s`" uri)
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))
    (reset! web-server_ server-map)))

(defonce router_ (atom nil))

(defn  stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn start!
  ([]
   (start-router!)
   (start-web-server! 10666))
  ([& [port]]
   (start-router!)
   (start-web-server! port)))
