(ns ^:figwheel-always vizard.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent defcomponentk defcomponentmethod]]
            [clojure.string :as str]
            [cljs.core.async :as async  :refer (<! >! put! chan)]
            [taoensso.encore :as encore :refer ()]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
            [taoensso.sente :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]
            [cljsjs.vega])
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"
                          :spec {"width" 400, "height" 200, "padding" {"top" 10, "left" 30, "bottom" 20, "right" 10}, "marks" [{"type" "rect", "from" {"data" "table"}, "properties" {"enter" {"x" {"scale" "xscale", "field" "category"}, "width" {"scale" "xscale", "band" true, "offset" -1}, "y" {"scale" "yscale", "field" "amount"}, "y2" {"scale" "yscale", "value" 0}}, "update" {"fill" {"value" "steelblue"}}, "hover" {"fill" {"value" "red"}}}} {"type" "text", "properties" {"enter" {"align" {"value" "center"}, "fill" {"value" "#333"}}, "update" {"x" {"scale" "xscale", "signal" "tooltip.category"}, "dx" {"scale" "xscale", "band" true, "mult" 0.5}, "y" {"scale" "yscale", "signal" "tooltip.amount", "offset" -5}, "text" {"signal" "tooltip.amount"}, "fillOpacity" {"rule" [{"predicate" {"name" "tooltip", "id" {"value" nil}}, "value" 0} {"value" 1}]}}}}], "scales" [{"name" "xscale", "type" "ordinal", "range" "width", "domain" {"data" "table", "field" "category"}} {"name" "yscale", "range" "height", "nice" true, "domain" {"data" "table", "field" "amount"}}], "axes" [{"type" "x", "scale" "xscale"} {"type" "y", "scale" "yscale"}], "signals" [{"name" "tooltip", "init" {}, "streams" [{"type" "rect:mouseover", "expr" "datum"} {"type" "rect:mouseout", "expr" "{}"}]}], "predicates" [{"name" "tooltip", "type" "==", "operands" [{"signal" "tooltip._id"} {"arg" "id"}]}], "data" [{"name" "table", "values" [{"category" "A", "amount" 28} {"category" "B", "amount" 55} {"category" "C", "amount" 43} {"category" "D", "amount" 91} {"category" "E", "amount" 81} {"category" "F", "amount" 53} {"category" "G", "amount" 19} {"category" "H", "amount" 87} {"category" "I", "amount" 52}]}]}}))

(def packer (sente-transit/get-flexi-packer :edn))

(let [{:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket! "/chsk"
                                                                       {:type :auto
                                                                        :packer packer})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defmulti event-msg-handler :id)

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (debugf "Event: %s" event)
  (event-msg-handler ev-msg))

(do
  (defmethod event-msg-handler :default
    [{:as ev-msg :keys [event]}]
    (debugf "Unhandled event: %s" event))

  (defmethod event-msg-handler :chsk/state
    [{:as ev-msg :keys [?data]}]
    (if (= ?data {:first-open? true})
      (debugf "Channel socket successfully established!")
      (debugf "Channel socket state change: %s" ?data)))

  (defmethod event-msg-handler :chsk/recv
    [{:as ev-msg :keys [?data]}]
    (let [[id msg] ?data]
      (case id
        :vizard/spec (swap! app-state assoc :spec msg)
        :default (debugf "Push event from server: %s" ?data))))

  (defmethod event-msg-handler :chsk/handshake
    [{:as ev-msg :keys [?data]}]
    (let [[?uid ?csrf-token ?handshake-data] ?data]
      (debugf "Handshake: %s" ?data))))

(def router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn start! []
  (start-router!))

(defn parse-vega-spec [spec elem]
  (js/vg.parse.spec
   (clj->js spec)
   (fn [chart]
     (try
       (.update (chart #js {:el elem :renderer "canvas"}))
       (catch js/Error e
         (.log js/console e))))))

(defcomponent application [data owner]
  (did-mount [_]
             (println (:spec data))
             (parse-vega-spec (:spec data) (om/get-node owner "vega")))
  (did-update [_ _ _]
              (println (:spec data))
              (parse-vega-spec (:spec data) (om/get-node owner "vega")))
  (render [_]
          (dom/span {:ref "vega"} "")))

(om/root
 application
 app-state
 {:target (. js/document (getElementById "app"))})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

(start!)
