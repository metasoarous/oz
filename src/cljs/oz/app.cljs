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

(let [packer (sente-transit/get-transit-packer)
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk"
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
      :oz.core/view-doc (swap! app-state merge {:view-spec msg :error nil})
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


(defn init []
  (start-router!)
  (rd/render [error-boundary [application app-state]]
             (. js/document (getElementById "app"))))

