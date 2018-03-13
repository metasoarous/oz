(ns ^:figwheel-always oz.core
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

(defonce app-state (r/atom {:text "Pay no attention to the man behind the curtain!"
                            :view-spec nil}))

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
      :oz.core/view-spec (swap! app-state assoc :view-spec msg)
      (debugf "Push event from server: %s" ?data))))


(def router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-client-chsk-router! ch-chsk event-msg-handler)))

(defn start! []
  (start-router!))

(defn render-vega-lite
  ([spec elem]
   (when spec
     (let [spec (clj->js spec)
           opts {:renderer "canvas"
                 :mode "vega-lite"}
           vega-spec (. js/vl (compile spec))]
       (log "Vega-lite translates to:")
       (log vega-spec)
       (-> (js/vegaEmbed elem spec (clj->js opts))
           (.then (fn [res]
                    #_(log res)
                    (. js/vegaTooltip (vegaLite (.-view res) spec))))
           (.catch (fn [err]
                     (log err))))))))

(defn render-vega [spec elem]
  (when spec
    (let [spec (clj->js spec)
          opts {:renderer "canvas"
                :mode "vega"}]
      (-> (js/vegaEmbed elem spec (clj->js opts))
          (.then (fn [res]
                   #_(log res)
                   (. js/vegaTooltip (vega (.-view res) spec))))
          (.catch (fn [err]
                    (log err)))))))

(defn vega-lite
  "Reagent component that renders vega-lite."
  [spec]
  (r/create-class
   {:display-name "vega-lite"
    :component-did-mount (fn [this]
                           (render-vega-lite spec (r/dom-node this)))
    :component-will-update (fn [this [_ new-spec]]
                             (render-vega-lite new-spec (r/dom-node this)))
    :reagent-render (fn [spec]
                      [:div#vis])}))


(defn vega
  "Reagent component that renders vega"
  [spec]
  (r/create-class
   {:display-name "vega"
    :component-did-mount (fn [this]
                           (render-vega spec (r/dom-node this)))
    :component-will-update (fn [this [_ new-spec]]
                             (render-vega new-spec (r/dom-node this)))
    :reagent-render (fn [spec]
                      [:div#vis])}))


(defn view-spec
  ;; should handle sharing data with nodes that need it?
  [spec]
  ;; prewalk spec, rendering special hiccup tags like :vega and :vega-lite, and potentially other composites,
  ;; rendering using the components above. Leave regular hiccup unchanged).
  ;; TODO finish writing; already hooked in below so will break now
  (clojure.walk/prewalk
    (fn [x] (if (and (coll? x) (#{:vega :vega-lite} (first x)))
              [(case (first x) :vega vega :vega-lite vega-lite)
               (reduce merge (rest x))]
              x))
    spec))


(defn application [app-state]
  (when-let [spec (:view-spec @app-state)]
    [view-spec spec]))

(r/render-component [application app-state]
                    (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

(start!)
