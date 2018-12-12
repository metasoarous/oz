(ns user
  (:require [oz.server :refer [start! stop!]]
            [oz.core :as oz]
            [cheshire.core :as json]
            [clojure.pprint :as pp]
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
  (oz/start-plot-server!)
  ;(stop!)

  ;; define a function for generating some dummy data
  (defn play-data [& names]
    (for [n names
          i (range 20)]
      {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))

  ;; Define a simple plot, inlining the data
  (def line-plot
    {:data {:values (play-data "monkey" "slipper" "broom")}
     :encoding {:x {:field "time"}
                :y {:field "quantity"}
                :color {:field "item" :type "nominal"}}
     :mark "line"})

  ;; Render the plot to the 
  (oz/v! line-plot)
  (oz/view! [:h1 "fuck you"])

  ;; We can also try publishing the plot like so (requires auth; see README.md for setup)
  (oz/publish! line-plot)
  ;; Then follow the vega-editor link.

  ;; Build a more intricate plot
  ;; (Note here also that we're doing the Right Thing (TM) and including the field types...)
  (def stacked-bar
    {:data {:values (play-data "munchkin" "witch" "dog" "lion" "tiger" "bear")}
     :mark "bar"
     :encoding {:x {:field "time"
                    :type "ordinal"}
                :y {:aggregate "sum"
                    :field "quantity"
                    :type "quantitative"}
                :color {:field "item"
                        :type "nominal"}}})

  ;; Render our new plot
  (oz/v! stacked-bar)


  ;; vega example
  (def vega-data (json/parse-string (slurp (clojure.java.io/resource "countour-lines.vega.json")))) 
  (oz/v! vega-data :mode :vega)

  ;; All together now
  ;; We can also use the `view!` function to view a composite of both charts, together with
  ;; some hiccup
  (def spec [:div
             [:h1 "Look ye and behold"]
             [:p "A couple of small charts"]
             [:div {:style {:display "flex" :flex-direction "row"}}
              [:vega-lite line-plot]
              [:vega-lite stacked-bar]]
             [:p "A wider, more expansive chart"]
             [:vega vega-data]
             [:h2 "If ever, oh ever a viz there was, the vizard of oz is one because, because, because..."]
             [:p "Because of the wonderful things it does"]])
  (oz/view! spec)

  ;; And finally, we can publish this document to a github gist and load via ozviz.io
  (oz/publish! spec)

  :end-examples)


