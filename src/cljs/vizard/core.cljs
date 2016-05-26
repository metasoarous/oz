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
            [cljsjs.vega]
            [cljsjs.vega-lite])
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(enable-console-print!)

(defonce app-state (atom {:text "Hail Satan!"
                          :spec nil
                          :vl-spec nil}))

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
    (let [[id msg] ?data
          compile-vl-spec (fn [vl-spec]
                            (try
                              (.-spec (js/vl.compile (clj->js vl-spec)))
                              (catch js/Error e
                                (.log js/console e))))]
      (case id
        :vizard/spec (swap! app-state assoc :spec (clj->js msg))
        :vizard/vl-spec (swap! app-state assoc :spec (compile-vl-spec msg) :vl-spec msg)
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
  (when spec
    (js/vg.parse.spec
     spec
     (fn [chart]
       (try
         (.update (chart #js {:el elem :renderer "canvas"}))
         (catch js/Error e
           (.log js/console e)))))))

(defcomponent application [data owner]
  (did-mount [_]
             (println (:vl-spec data))
             (parse-vega-spec (:spec data) (om/get-node owner "vega")))
  (did-update [_ _ _]
              (println (:vl-spec data))
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
