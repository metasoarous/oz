(ns ^:figwheel-always oz.core
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [cljsjs.vega]
            [cljsjs.vega-lite]
            [cljsjs.vega-embed]
            [cljsjs.vega-tooltip]))

;(log/set-level! :info)
(enable-console-print!)

(defn- ^:no-doc log [a-thing]
  (.log js/console a-thing))


(defn ^:no-doc render-vega-lite
  ([spec elem] (render-vega-lite spec elem {}))
  ([spec elem opts]
   (when spec
     (let [spec (clj->js spec)
           opts (merge {:renderer "canvas"
                        :mode "vega-lite"}
                       opts)]
       (-> (js/vegaEmbed elem spec (clj->js opts))
           (.then (fn [res]
                    #_(log res)
                    (. js/vegaTooltip (vegaLite (.-view res) spec))))
           (.catch (fn [err]
                     (log err))))))))

(defn render-vega
  ([spec elem] (render-vega spec elem {}))
  ([spec elem opts]
   (when spec
     (let [spec (clj->js spec)
           opts (merge {:renderer "canvas"
                        :mode "vega"}
                       opts)]
       (-> (js/vegaEmbed elem spec (clj->js opts))
           (.then (fn [res]
                    #_(log res)
                    (. js/vegaTooltip (vega (.-view res) spec))))
           (.catch (fn [err]
                     (log err))))))))

(defn vega-lite
  "Reagent component that renders vega-lite."
  ([spec] (vega-lite spec {}))
  ([spec opts]
   (r/create-class
    {:display-name "vega-lite"
     :component-did-mount (fn [this]
                            (render-vega-lite spec (r/dom-node this) opts))
     :component-will-update (fn [this [_ new-spec]]
                              (render-vega-lite new-spec (r/dom-node this) opts))
     :reagent-render (fn [spec]
                       [:div#vis])})))


(defn vega
  "Reagent component that renders vega"
  ([spec] (vega spec {}))
  ([spec opts]
   (r/create-class
    {:display-name "vega"
     :component-did-mount (fn [this]
                            (render-vega spec (r/dom-node this) opts))
     :component-will-update (fn [this [_ new-spec]]
                              (render-vega new-spec (r/dom-node this) opts))
     :reagent-render (fn [spec]
                       [:div#vis])})))


(defn ^:no-doc view-spec
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


