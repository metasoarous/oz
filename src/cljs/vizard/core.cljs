(ns ^:figwheel-always vizard.core
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [cljs.core.async :as async  :refer (<! >! put! chan)]
            [taoensso.encore :as encore :refer-macros (have have?)]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
            [taoensso.sente :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]
            [cljsjs.vega]
            [cljsjs.vega-lite]
            [cljsjs.vega-embed]
            [cljsjs.vega-tooltip])
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(timbre/set-level! :info)
(enable-console-print!)

(defn log [a-thing]
  (.log js/console a-thing))

(defonce app-state (r/atom {:text "Hail Satan!"
                            :spec nil
                            :vl-spec nil}))

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

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (let [[id msg] ?data]
    (case id
      :vizard/vl-spec (swap! app-state assoc :vl-spec (clj->js msg))
      (debugf "Push event from server: %s" ?data))))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (debugf "Handshake: %s" ?data)))

(def router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-client-chsk-router! ch-chsk event-msg-handler)))

(defn start! []
  (start-router!))

(defn parse-vl-spec [spec elem]
  (when spec
    (let [opts {:renderer "canvas"
                :mode "vega-lite"}]
      (-> (js/vegaEmbed elem spec (clj->js opts))
          (.then (fn [res]
                   #_(log res)
                   (. js/vegaTooltip (vegaLite (.-view res) spec))))
          (.catch (fn [err]
                    (log err)))))))

(defn vega-lite
  "Reagent component that renders vega-lite."
  [spec]
  (r/create-class
   {:display-name "vega-lite"
    :component-did-mount (fn [this]
                           (parse-vl-spec spec (r/dom-node this)))
    :component-will-update (fn [this [_ new-spec]]
                             (parse-vl-spec new-spec (r/dom-node this)))
    :reagent-render (fn [spec]
                      [:div#vis])}))

(defn application [app-state]
  (when-let [spec (:vl-spec @app-state)]
    [vega-lite spec]))

(r/render-component [application app-state]
                    (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

(start!)
