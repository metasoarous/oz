(ns vizard.vega
  (:require [cheshire.core :as json]))

;; output vega spec as json
(defn dump
  ([v]
   (json/generate-string v))
  ([v filename]
   (spit filename (json/generate-string v))))

;; vega stuff

;; scales
(defn scale
  [name range & args]
  (let [opts (apply hash-map args)]
    (assoc opts
           :name name :range range)))

(defn scales
  [v & scls]
  (assoc v
         :scales
         (vec (map (partial apply scale) scls))))

;; axes
(defn axis
  [type scale & args]
  (let [opts (apply hash-map args)]
    (assoc opts :type type :scale scale)))

(defn axes
  [v & axs]
  (assoc v
         :axes
         (map (partial apply axis) axs)))

;; properties (which go into marks)
(defn property
  [pset p & args]
  (let [opts (apply hash-map args)]
    (assoc pset p
           opts)))

(defn property-set
  [psets set-name pset]
  (assoc psets set-name
         (reduce #(apply (partial property %1) %2) {} pset)))


(defn properties
  [& psets]
  (reduce #(apply (partial property-set %1) %2)
          {} (partition 2 psets)))

;; marks

(defn marks
  [v type data-name props]
  (assoc v
         :marks
         [{:type type,
           :from  {"data" data-name},
           :properties props}]))

;; top-level options
(defn mk-padding
  ([width]
   (mk-padding width width width width))
  ([width height]
   (mk-padding width height width height))
  ([top left bottom right]
   (zipmap [:top :left :bottom :right]
           [top left bottom right])))

(defn padding
  [padding-vec]
  (apply mk-padding padding-vec))

(defn viewport
  [v width height]
  (assoc v
         :viewport [width height]))

;; ordering is important:
;; marks depends on data and scales
;; scales depends on data
(defmacro vega
  [data axes marks scales & [opts]]
  `(-> (assoc (merge {:height 200 :width 400} ~opts)
              :data ~data)
       ~axes
       ~marks
       ~scales))

(defn mk-data [name values]
  {:name name
   :values values})

;; standard charts
(defn bar-chart
  [data-vals]
  (let [data-name "table"]
    (vega
     [(mk-data data-name data-vals)]
     (axes ["x" "x"] ["y" "y"])
     (marks :rect data-name
            (properties :enter [[:x :scale "x" :field "data.x"]
                                [:width :scale "x" :band true :offset -1]
                                [:y :scale "y" :field "data.y"]
                                [:y2 :scale "y" :value 0]]
                        :update [[:fill :value "steelblue"]]
                        :hover  [[:fill :value "red"]]))
     (scales ["x" :width :type :ordinal
              :domain {:data data-name, :field "data.x"}]
             ["y" :height :nice true
              :domain {:data data-name, :field "data.y"}])
     {:padding (padding [10 30 20 10])})))


(defn line-chart
  [data-vals]
  (let [data-name "table"]
    (vega
     [(mk-data data-name data-vals)]
     (axes ["x" "x"] ["y" "y"])
     (marks :line data-name
            (properties :enter [[:x :scale "x" :field "data.x"]
                                [:y :scale "y" :field "data.y"]
                                [:stroke :value "red"]
                                [:strokeWidth :value 2]]))
     (scales ["x" :width :points true
              :domain {:data data-name, :field "data.x"}]
             ["y" :height :nice true
              :domain {:data data-name, :field "data.y"}])
     {:padding (padding [10 40 20 40])})))


;; looking at torcho trending data
(defn intent-scatter
  [data-vals]
  (let [data-name "points"]
    (vega
     [(mk-data data-name data-vals)]
     (axes ["x" "x" :title "temperature"]
           ["y" "y" :title "current temperature"])
     (marks :text data-name
            (properties :enter [[:x :scale "x" :field "data.x"]
                                [:y :scale "y" :field "data.y"]
                                [:text :field "data.text"]
                                [:stroke :value "steelblue"]
                                [:fill :value "steelblue"]
                                [:fontSize :value 12]]
                        :hover [[:fontSize :value 28]
                                [:stroke :value "black"]]
                        :update [[:fontSize :value 12]
                                 [:stroke :value "steelblue"]]))
     (scales ["x" :width :points true
              :domain {:data data-name, :field "data.x"}]
             ["y" :height :nice true
              :domain {:data data-name, :field "data.y"}])
     {:padding (padding [50 50 50 50])
      :height 600
      :width 800})))

(comment
  (let [raw-data (with-drpc "stormmaster-0.dev.yb0t.com" 3772 10000
                   (drpc-execute "torcho-trends" {:top_k 100 :cooling_rate 0.5}))
        name-map {"intent" "text"
                  "temperature" "x"
                  "current_temperature" "y"}
        ds (map #(rename-keys % name-map) (drop 20 raw-data))]
    (output (scatter ds))))

;; example usages based on random data

(comment
  ;; example barchart with random data
  (use 'farnsworth.vega)
  ;; at this point, http://localhost:3000 should load a blank page
  (defn gen-ordinal-values []
    [{"x" "A", "y" (rand-int 100)},
     {"x" "B", "y" (rand-int 100)},{"x" "C", "y" 43},
     {"x" "D", "y" 91},
     {"x" "E", "y" 81}, {"x" "F", "y" (rand-int 100)},
     {"x" "G", "y" (rand-int 100)},
     {"x" "H", "y" 87}, {"x" "I", "y" (rand-int 100)}])
  ;; each time you execute this, the browser
  ;; should automatically refresh
  ;; with a plot of the latest random data
  (output (bar-chart (gen-ordinal-values))))

(comment
  ;; example linechart with random data, similar to above
  (use 'farnsworth.vega)
  (defn gen-numeric-values []
    [{"x" 1, "y" (rand-int 100)},
     {"x" 2, "y" (rand-int 100)},{"x" "C", "y" 43},
     {"x" 3, "y" 91},
     {"x" 4, "y" 81}, {"x" "F", "y" (rand-int 100)},
     {"x" 5, "y" (rand-int 100)},
     {"x" 6, "y" 87}, {"x" "I", "y" (rand-int 100)}])
  (output (line-chart (gen-numeric-values))))

;; Jim's stuff
;; TODO integrate/generalize

(defn mk-points [name xs ys]
  (map (fn [x y] {:x x :y y :name name}) xs ys))

(defn marks-facet
  [v type data-name props]
  (assoc v :marks [{:type "group"
                    :from {:data data-name
                           :transform [{:type "facet" :keys ["data.name"]}]}
                    :marks [{:type type
                             :properties props}
                            {:type "text"
                             :from {:data "title"}
                             :name "title"
                             :properties {:enter {:align {:value "center"}
                                                  :x {:value 200}
                                                  :y {:value 0}
                                                  :text {:field "data.label"}
                                                  :baseline {:value "bottom"}
                                                  :dy {:value -20}
                                                  :fill {:value "#000"}
                                                  :font {:value "Helvetica Neue"}
                                                  :fontSize {:value 14}}}}]}]))

(defn legend [title & {:keys [values]}]
  [{:fill "color"
    :title title
    :offset 0
    :values values
    :properties {:symbols {:fillOpacity {:value 0.5},
                           :stroke {:value "transparent"}}}}])

(defn multi-line-chart
  [data-vals &
   {:keys [x-axis y-axis x-time? legend? chart-title legend-title
           x-format y-format x-title-offset y-title-offset
           width height pad-top pad-left pad-bottom pad-right
           legend-values]
    :or {x-axis "" y-axis "" x-time? false legend? false
         x-title-offset 35 y-title-offset 50 width 600 height 400
         pad-top 20 pad-left 60 pad-bottom 60 pad-right 60}}]
  (let [data-name "chart-data"
        data-vals (sort-by (juxt :x :name) data-vals)
        data (conj (mk-data data-name data-vals)
                   (when x-time? {:format {:parse {:x "date"}}}))]
    (vega
     [data {:name "title" :values [{:label chart-title}]}]
     (axes ["x" "x" :title x-axis
            :offset 5 :format x-format :titleOffset x-title-offset
            :properties (when x-time?
                          {:labels {:angle {:value -45}
                                    :align {:value "right"}}})]
           ["y" "y" :title y-axis
            :offset 5 :format y-format :titleOffset y-title-offset])
     (marks-facet :line data-name
                  (properties :enter [[:x :scale "x" :field "data.x"]
                                      [:y :scale "y" :field "data.y"]
                                      [:stroke :scale "color" :field "data.name"]
                                      [:strokeWidth :value 2]]))
     (scales (concat [:x :width :domain {:data data-name :field "data.x"}]
                     (when x-time? [:type "time" :nice nil]))
             [:y :height :domain {:data data-name :field "data.y"}]
             [:color "category10" :type "ordinal"])
     {:legends (when legend? (legend legend-title :values legend-values))
      :padding (padding [pad-top pad-left pad-bottom pad-right])
      :height height
      :width width})))

(defn multi-line-log-chart
  [data-vals &
   {:keys [x-axis y-axis x-time? legend? chart-title legend-title
           x-format y-format x-title-offset y-title-offset
           width height pad-top pad-left pad-bottom pad-right
           x-log? y-log? x-domain-min y-domain-min]
    :or {x-axis "" y-axis "" x-time? false legend? false
         x-title-offset 35 y-title-offset 50 width 600 height 400
         pad-top 20 pad-left 60 pad-bottom 60 pad-right 60
         x-log? false y-log? false x-domain-min 1 y-domain-min 1}}]
  (let [chart (multi-line-chart data-vals
                                :x-axis x-axis :y-axis y-axis :x-time? x-time? :legend? legend?
                                :chart-title chart-title :legend-title legend-title
                                :x-format x-format :y-format y-format
                                :x-title-offset x-title-offset :y-title-offset y-title-offset
                                :width width :height height :pad-top pad-top :pad-left pad-left
                                :pad-bottom pad-bottom :pad-right pad-right)
        chart (if y-log?
                (-> chart
                    (assoc-in [:marks 0 :marks 0 :properties :enter :y2]
                              {:scale "y" :field "data.y"})
                    (assoc-in [:scales 1 :type] "log")
                    (assoc-in [:scales 1 :domainMin] y-domain-min))
                chart)]

    (if x-log?
      (-> chart
          (assoc-in [:marks 0 :marks 0 :properties :enter :x2]
                    {:scale "x" :field "data.x"})
          (assoc-in [:scales 0 :type] "log")
          (assoc-in [:scales 0 :domainMin] x-domain-min))
      chart)))



(defn bar-marks [v scale-type]
  (assoc v :marks [{:type "group"
                    :from {:data "table"
                           :transform [{:type "facet" :keys ["data.category"]}]}
                    :properties {:enter {:y {:scale "cat"
                                             :field "key"}
                                         :height {:scale "cat"
                                                  :band true}}}
                    :scales [{:name "pos"
                              :type "ordinal"
                              :range "height"
                              :domain {:field "data.position"}}]
                    :marks [{:type "rect"
                             :properties {:enter {:y {:scale "pos" :field "data.position"}
                                                  :height {:scale "pos" :band true}
                                                  :x {:scale "val" :field "data.value"}
                                                  :x2 (if (= scale-type "log")
                                                        {:value 1 :offset -1}
                                                        {:scale "val" :value 0})
                                                  :fill {:scale "color" :field "data.position"}}}}
                            {:type "text"
                             :properties {:enter {:y {:scale "pos" :field "data.position"}
                                                  :dy {:scale "pos" :band true :mult 0.5}
                                                  :x {:scale "val" :field "data.value" :offset 40}
                                                  :fill {:value "black"}
                                                  :align {:value "right"}
                                                  :baseline {:value "middle"}
                                                  :text {:field "data.value"}}}}]}]))


(defn multi-bar-chart
  [data-vals & {:keys [scale-type]
                :or {scale-type "linear"}}]
  (let [data-name "table"]
    (vega [(mk-data data-name data-vals)]
          (axes ["y" "cat" :offset 5 :tickSize 0 :tickPadding 8]
                ["x" "val" :offset 5])
          (bar-marks scale-type)
          (scales [:cat :height
                   :name "cat" :type "ordinal"
                   :padding 0.2
                   :domain {:data "table" :field "data.category"}]
                  [:val :width
                   :name "val"
                   :type scale-type
                   :round true :nice true
                   :domain {:data "table" :field "data.value"}]
                  [:color "category20" :type "ordinal" ]))))



(defn my-bar-chart
  [data-vals & {:keys [x-axis y-axis chart-title
                       pad-left pad-top pad-right pad-bottom]
                :or {pad-left 40 pad-top 40 pad-right 40 pad-bottom 40}}]
  (let [data-name "table"
        data (mk-data data-name data-vals)]
    (vega
     [data {:name "title" :values [{:label chart-title}]}]
     (axes ["x" "x" :title x-axis] ["y" "y" :title y-axis])
     (marks :rect data-name
            (properties :enter [[:x :scale "x" :field "data.x"]
                                [:width :scale "x" :band true :offset -1]
                                [:y :scale "y" :field "data.y"]
                                [:y2 :scale "y" :value 0]]
                        :update [[:fill :value "steelblue"]]
                        :hover  [[:fill :value "red"]]))
     (scales ["x" :width :type :ordinal
              :domain {:data data-name, :field "data.x"}]
             ["y" :height :nice true
              :domain {:data data-name, :field "data.y"}])
     {:padding (padding [pad-top pad-left pad-bottom pad-right])
      :height 200
      :width 400})))

(defn histogram-marks [v type data-name props]
  (assoc v
         :marks
         [{:type type,
           :from  {"data" data-name},
           :properties props}
          {:type "text"
           :from {:data "title"}
           :name "title"
           :properties {:enter {:align {:value "center"}
                                :x {:value 200}
                                :y {:value 0}
                                :text {:field "data.label"}
                                :baseline {:value "bottom"}
                                :dy {:value -20}
                                :fill {:value "#000"}
                                :font {:value "Helvetica Neue"}
                                :fontSize {:value 14}}}}]))

(defn histogram-chart
  [data-vals & {:keys [height width x-axis y-axis chart-title bin-size offset]
                :or {height 400 width 800
                     x-axis "x" y-axis "y" chart-title "title"}}]
  (let [data-name "histogram-data"
        n (count data-vals)
        w (int (/ width (double n)))
        data (mk-data data-name data-vals)
        offset (or offset (- bin-size))]
    (vega
     [data {:name "title" :values [{:label chart-title}]}]
     (axes ["x" "x" :title x-axis
            :properties {:labels {:angle {:value 45}
                                  :align {:value "left"}}}]
           ["y" "y" :title y-axis :titleOffset 55])
     (histogram-marks :rect data-name
                      (properties :enter [[:x :scale "x" :field "data.x"]
                                          [:width :value bin-size
                                           :scale "x"
                                           :offset offset]
                                          [:y :scale "y" :field "data.y"]
                                          [:y2 :scale "y" :value 0]]
                                  :update [[:fill :value "steelblue"]]))
     (scales [:x :width :type :linear :points true
              :domain {:data data-name :field "data.x"}]
             [:y :height :nice true :type :linear
              :domain {:data data-name :field "data.y"}])
     {:padding (padding [40 70 50 10])
      :height height
      :width width})))

(defn stacked-marks-facet
  [v data-name props]
  (assoc v :marks [{:type "group"
                    :from {:data data-name
                           :transform [{:type "facet" :keys ["data.name"]}
                                       {:type "stack" :point "data.x" :height "data.y"}]}
                    :marks [{:type "area"
                             :properties props}
                            {:type "text"
                             :from {:data "title"}
                             :name "title"
                             :properties {:enter {:align {:value "center"}
                                                  :x {:value 200}
                                                  :y {:value 0}
                                                  :text {:field "data.label"}
                                                  :baseline {:value "bottom"}
                                                  :dy {:value -20}
                                                  :fill {:value "#000"}
                                                  :font {:value "Helvetica Neue"}
                                                  :fontSize {:value 14}}}}]}]))

(defn stacked-area-chart [data-vals &
                          {:keys [chart-title legend-title legend?
                                  x-scale x-zero? interpolate
                                  x-axis y-axis width height
                                  pad-top pad-left pad-bottom pad-right
                                  x-title-offset y-title-offset]
                           :or {width 600 height 400 x-scale "linear"
                                interpolate "linear" x-zero? false
                                x-title-offset 35 y-title-offset 50
                                pad-top 40 pad-left 60 pad-bottom 60 pad-right 60}}]
  (let [data-name "stacked-area"
        data-vals (sort-by (juxt :x :name) data-vals)
        data [(mk-data data-name data-vals)
              {:name "stats" :source data-name
               :transform [{:type "facet" :keys ["data.x"]}
                           {:type "stats" :value "data.y"}]}
              {:name "title" :values [{:label chart-title}]}]]
    (vega
     data
     (axes ["x" "x" :title x-axis :titleOffset x-title-offset]
           ["y" "y" :title y-axis :titleOffset y-title-offset])
     (stacked-marks-facet data-name
                          (properties :enter [[:x :scale "x" :field "data.x"]
                                              [:y :scale "y" :field "y"]
                                              [:y2 :scale "y" :field "y2"]
                                              [:interpolate :value interpolate]
                                              [:fill :scale "color" :field "data.name"]]))
     (scales [:x :width :domain {:data data-name :field "data.x"}
              :zero x-zero?
              :type x-scale]
             [:y :height :domain {:data "stats" :field "sum"}
              :type "linear" :nice true]
             [:color "category20" :type "ordinal"])
     {:legends (when legend? (legend legend-title))
      :padding (padding [pad-top pad-left pad-bottom pad-right])
      :height height
      :width width})))

(comment
  (let [data [28 43 81 19 52 24 87 17 68 49]
        more-data [55 91 53 87 48 49 66 27 16 15]
        data-pts (mk-points "0" (map str (range (count data))) data)
        more-pts (mk-points "1" (map str (range (count more-data))) more-data)]
    (stacked-area-chart (concat data-pts more-pts) :legend? true
                        :chart-title "a thing"
                        :x-axis "the x axis"
                        :y-axis "the y axis")))

(comment
  (let [data [41.7 24.0 32.3 37.3
              46.2 29.3 36.5 43.0
              48.9 31.2 37.7 40.4
              51.2 31.9 41.0 43.8
              55.6 33.9 42.1 45.6
              59.8 35.2 44.3 47.9]
        in-forecasts [43.004091612920604 24.38268565383658 32.609457036816195 37.93744225814007
                      47.41392312739937 30.2825953225354 37.55092467635864 44.04053997726309
                      49.92032261700378 31.799882102303194 38.23792365930591 40.33812300016852
                      51.31825791977009 32.34941381860498 41.780861915195445 44.37089133574706
                      56.26938143321972 34.465633738019065 42.69876601957208 45.92919826205611
                      60.524217713983454 35.770219857902724 44.873351366654454 48.279592260456894]
        out-forecasts [59.579546278620285 35.43154196555864 44.351050406447364 48.92335307904717]
        forecasts (concat in-forecasts out-forecasts)
        data-pts (mk-points "data" (range (count data)) data)
        forecast-pts (mk-points "forecasts" (range (count forecasts)) forecasts)]
    (plot (multi-line-chart (concat data-pts forecast-pts)
                            :legend? true
                            :x-axis "x axis"
                            :y-axis "y axis")))
  (let [data [41.7 24.0 32.3 37.3
              46.2 29.3 36.5 43.0
              48.9 31.2 37.7 40.4
              51.2 31.9 41.0 43.8
              55.6 33.9 42.1 45.6
              59.8 35.2 44.3 47.9]
        in-forecasts [43.004091612920604 24.38268565383658 32.609457036816195 37.93744225814007
                      47.41392312739937 30.2825953225354 37.55092467635864 44.04053997726309
                      49.92032261700378 31.799882102303194 38.23792365930591 40.33812300016852
                      51.31825791977009 32.34941381860498 41.780861915195445 44.37089133574706
                      56.26938143321972 34.465633738019065 42.69876601957208 45.92919826205611
                      60.524217713983454 35.770219857902724 44.873351366654454 48.279592260456894]
        out-forecasts [59.579546278620285 35.43154196555864 44.351050406447364 48.92335307904717]
        dates ["2008-03" "2008-06" "2008-09" "2008-12"
               "2009-03" "2009-06" "2009-09" "2009-12"
               "2010-03" "2010-06" "2010-09" "2010-12"
               "2011-03" "2011-06" "2011-09" "2011-12"
               "2012-03" "2012-06" "2012-09" "2012-12"
               "2013-03" "2013-06" "2013-09" "2013-12"]
        more-dates ["2008-03" "2008-06" "2008-09" "2008-12"
                    "2009-03" "2009-06" "2009-09" "2009-12"
                    "2010-03" "2010-06" "2010-09" "2010-12"
                    "2011-03" "2011-06" "2011-09" "2011-12"
                    "2012-03" "2012-06" "2012-09" "2012-12"
                    "2013-03" "2013-06" "2013-09" "2013-12"
                    "2014-03" "2014-06" "2014-09" "2014-12"]
        forecasts (concat in-forecasts out-forecasts)
        data-pts (mk-points "data" dates data)
        forecast-pts (mk-points "forecasts" more-dates forecasts)]
    (plot (multi-line-chart (concat data-pts forecast-pts)
                            :legend? true
                            :x-time? true
                            :x-axis "x axis"
                            :y-axis "y axis"))))

;; (comment
;;   (let [data [{:category :ffd8, :position 0, :value 188229}
;;               {:category :5c52, :position 0, :value 106639}
;;               {:category :fe63, :position 0, :value 44562}
;;               {:category :ffd8, :position 1, :value 16759}
;;               {:category :fe63, :position 1, :value 1206}
;;               {:category :5c52, :position 1, :value 7902}]]
;;     (plot (multi-bar-chart data :scale-type "linear"))))
