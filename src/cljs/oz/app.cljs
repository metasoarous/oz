(ns ^:no-doc ^:figwheel-always oz.app
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.core.async :as async  :refer (<! >! put! chan)]
            [taoensso.encore :as encore :refer-macros (have have?)]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
            [clojure.string :as str]
            [clojure.stacktrace :as st]
            [taoensso.sente :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]
            [oz.core :as core])
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(timbre/set-level! :info)
(enable-console-print!)

(defonce app-state (r/atom {:text "Pay no attention to the man behind the curtain!"
                            :view-spec nil
                            :error nil}))

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(when-not ?csrf-token
  (js/console.log "WARNING! No csrf token found! Websocket connection will likely fail."))

(let [packer (sente-transit/get-transit-packer)
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk"
                                         ?csrf-token
                                         {:type :auto
                                          :packer packer})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defmulti -event-msg-handler :id)

(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (debugf "Event: %s" event) []
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default
  [{:as ev-msg :keys [event]}]
  (debugf "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] (have vector? ?data)]
    (if (:first-open? new-state-map)
      (debugf "Channel socket successfully established!: %s" ?data)
      (debugf "Channel socket state change: %s" ?data))))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (debugf "Handshake: %s" ?data)))



;; This is the main event handler; If we want to do cool things with other kinds of data going back and forth,
;; this is where we'll inject it.
(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (let [[id msg] ?data]
    (case id
      :oz.core/view-doc
      (do (swap! app-state merge {:view-spec msg :error nil})
          (async/go
            (<! (async/timeout 50))
            (.typeset js/MathJax)))
      (debugf "Push event from server: %s" ?data))))


(def router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-client-chsk-router! ch-chsk event-msg-handler)))


(defn application [app-state]
  (if-let [spec (:view-spec @app-state)]
    [core/live-view spec]
    [:div
      [:h1 "Waiting for first spec to load..."]
      [:p "This may take a second the first time if you call a plot function, unless you first call " [:code '(oz/start-server!)] "."]]))

(defn error-boundary
  [component]
  (r/create-class
    {:component-did-catch (fn [this e info]
                            (swap! app-state assoc :error e))
     :reagent-render (fn [comp]
                       (if-let [error (:error @app-state)]
                         [:div
                          [:h2 "Unable to process document!"]
                          [:h3 "Error:"]
                          [:code (pr-str error)]]
                         comp))}))

(add-watch chsk-state ::chsk-connected?
  (fn [_ _ _ old-value new-value]
    ;; For some reason new-value v old-value appear to be switched
    ;; So just going to use a fresh deref on the atom value
    (when (:open? @chsk-state)
      (js/console.log "chsk now open; sending connection established message.")
      (go
        (<! (async/timeout 1000))
        (chsk-send! [::connection-established])))))
    ;; TODO Consider whether we should initialize our own reconnection loop, so that we don't necessarily have
    ;; open up a new window when we restart the process (and can reconnect faster). Main downside is if you
    ;; don't realize you still have an old tab open that nabs the session, it won't open a new window; could be
    ;; confusing
    ;; This isn't right... need to have a :reconnecting state separately somewhere since this ends up getting
    ;; triggered in an insane loop due to existing sente reconnection attempts stacking
      ;(do
        ;(js/console.log "oz chsk closed; attempting to reestablish connection")))))
        ;(go
          ;(<! (async/timeout 2000))
          ;(sente/chsk-connect! chsk))))))

(async/go-loop []
  (<! (async/timeout 2000))
  (.typeset js/MathJax)
  (recur))

(defn init []
  (start-router!)
  (rd/render [error-boundary [application app-state]]
             (. js/document (getElementById "app"))))

(init)
