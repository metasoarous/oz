(ns user
  (:require [oz.server :refer [start! stop!]]
            [oz.core :as oz]
            [cheshire.core :as json]
            [figwheel-sidecar.repl-api :as figwheel]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn run []
  (figwheel/start-figwheel!))

(defn do-it-fools! []
  (run)
  (start!))

(def browser-repl figwheel/cljs-repl)


;; Here is some example usage you can play with at the repl
(comment

  ;; Start the plot server
  ;(do-it-fools!) ;; for figwheel dev
  (oz/start-plot-server! 8776)

  ;; define a function for generating some dummy data
  (defn group-data [& names]
    (apply concat
      (for [n names]
        (map-indexed (fn [i x] {:x i :y x :col n}) (take 20 (repeatedly #(rand-int 100)))))))


  ;; Define a simple plot, inlining the data
  (def line-plot
    {:data {:values (group-data "monkey" "slipper" "broom")}
     :encoding {:x {:field "x"}
                :y {:field "y"}
                :color {:field "col" :type "nominal"}}
     :mark "line"})

  ;; Render the plot to the 
  (oz/v! line-plot)

  ;; We can also try publishing the plot like so (follow the vega-editor link)
  (oz/publish-plot! line-plot)


  ;; Build a more intricate plot
  (def stacked-bar
    {:data {:values (group-data "munchkin" "witch" "dog" "lion" "tiger" "bear")}
     :mark "bar"
     :encoding {:x {:field "x"
                    :type "ordinal"}
                :y {:aggregate "sum"
                    :field "y"
                    :type "quantitative"}
                :color {:field "col"
                        :type "nominal"}}})

  ;; Render our new plot
  (oz/v! stacked-bar)


  ;; vega example
  (def vega-data (json/parse-string (slurp (clojure.java.io/resource "example-cars-plot.vega.json")))) 
  (oz/v! vega-data :mode :vega)

  ;; All together now
  ;; We can also use the `view!` function to view a composite of both charts, together with
  ;; some hiccup
  (oz/view! [:div
             [:h1 "Look ye and behold"]
             [:p "A couple of small charts"]
             [:div {:style {:display "flex" :flex-direction "row"}}
              [:vega-lite line-plot]
              [:vega vega-data]]
             [:p "A wider, more expansive chart"]
             [:vega-lite stacked-bar]
             [:h2 "If ever, oh ever a viz there was, the vizard of oz is one because, because, because..."]
             [:p "Because of the wonderful things it does"]])


  :end-examples)


