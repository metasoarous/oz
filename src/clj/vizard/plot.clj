(ns vizard.plot
  (:require [cheshire.core :as json]
            [com.rpl.specter :as s]))

;; scales

(defn scale
  [name range & args]
  (let [opts (apply hash-map args)]
    (assoc opts :name name :range range)))

(defn scales
  [& scls]
  (vec (map (partial apply scale) scls)))

;; axes

(defn axis
  [type scale & args]
  (let [opts (apply hash-map args)]
    (assoc opts :type type :scale scale)))

(defn axes
  [& axs]
  (vec (map (partial apply axis) axs)))

;; legends

(defn legend
  [type scale & args]
  (let [opts (apply hash-map args)]
    (assoc opts type scale)))

(defn legends
  [& ls]
  (vec (map (partial apply legend) ls)))

;; properties

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

;; transforms

(defn transform
  [type & args]
  (let [opts (apply hash-map args)]
    (assoc opts :type type)))

(defn transforms
  [& tforms]
  (vec (map (partial apply transform) tforms)))

;; from

(defn from
  [data-name & tforms]
  (if tforms
    (assoc {:data data-name} :transform (apply transforms tforms))
    {:data data-name}))

;; marks

(defn mark
  [type & args]
  (let [opts (apply hash-map args)]
    (assoc opts :type type)))

(defn marks
  [& ms]
  (vec (map (partial apply mark) ms)))

(defn group-mark
  [from & args]
  (let [opts (apply hash-map args)]
    [(merge {:type :group :from from} opts)]))

;; data

(defn d [name & args]
  (let [opts (apply hash-map args)]
    (assoc opts :name name)))

(defn data [& ds]
  (vec (map (partial apply d) ds)))

;; TODO: legends

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
  (assoc v :viewport [width height]))

;; TODO: make this like scales and transforms
(defn mk-data [aname values]
  {:name (name aname)
   :values values})

(defn vega
  [& args]
  (let [opts (apply hash-map args)]
    (merge {:height 500 :width 960} opts)))

;; plots

(defmulti vizard (fn [config _]
                   (:mark-type config)))

(defmethod vizard :line
  [config data-vals]
  (let [{:keys [mark-type x y g time? legend? color]
         :or {x "x"
              y "y"
              g "col"
              time? false
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals])
           :axes (axes [:x "x"] [:y "y"])
           :marks (group-mark
                   (from data-name [:facet :groupby [g]])
                   :marks (marks [:line
                                  :properties (properties :enter [[:x :scale "x" :field x]
                                                                  [:y :scale "y" :field y]
                                                                  [:stroke :scale "color" :field g]
                                                                  [:strokeWidth :value 2]])]))
           :scales (scales [:x :width
                            :type (if time? "time" "linear")
                            :domain {:data data-name :field x}]
                           [:y :height
                            :nice true
                            :domain {:data data-name :field y}]
                           [:color color
                            :type "ordinal"
                            :domain {:data data-name :field g}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :scatter
  [config data-vals]
  (let [{:keys [mark-type x y g time? legend? color]
         :or {x "x"
              y "y"
              g "col"
              time? false
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals])
           :axes (axes [:x "x"] [:y "y"])
           :marks (group-mark
                   (from data-name [:facet :groupby [g]])
                   :marks (marks [:symbol
                                  :properties (properties :enter [[:x :scale "x" :field x]
                                                                  [:y :scale "y" :field y]
                                                                  [:size :value 100]
                                                                  [:fill :scale "color" :field g]])]))
           :scales (scales [:x :width
                            :type (if time? "time" "linear")
                            :domain {:data data-name :field x}]
                           [:y :height
                            :nice true
                            :domain {:data data-name :field y}]
                           [:color color
                            :type "ordinal"
                            :domain {:data data-name :field g}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :bar
  [config data-vals]
  (let [{:keys [mark-type x y g legend? color]
         :or {x "x"
              y "y"
              g "col"
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals]
                       [:stats
                        :source data-name
                        :transform (transforms [:aggregate
                                                :groupby [x]
                                                :summarize [{:field y :ops ["sum"]}]])])
           :axes (axes [:x "x"] [:y "y"])
           :marks (marks [:rect
                          :from (from data-name [:stack :groupby [x] :sortby [g] :field y])
                          :properties (properties :enter [[:x :scale "x" :field x]
                                                          [:width :scale "x" :band true :offset -1]
                                                          [:y :scale "y" :field "layout_start"]
                                                          [:y2 :scale "y" :field "layout_end"]
                                                          [:fill :scale "color" :field g]])])
           :scales (scales [:x :width :type "ordinal"
                            :domain {:data data-name :field x}]
                           [:y :height :type "linear" :nice true
                            :domain {:data "stats" :field (str "sum_" y)}]
                           [:color color
                            :type "ordinal"
                            :domain {:data data-name :field g}])
           :padding "auto")]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :area
  [config data-vals]
  (let [{:keys [mark-type x y g time? legend? color]
         :or {x "x"
              y "y"
              g "col"
              time? false
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals]
                       [:stats
                        :source data-name
                        :transform (transforms [:aggregate
                                                :groupby [x]
                                                :summarize [{:field y :ops ["sum"]}]])])
           :axes (axes [:x "x"] [:y "y"])
           :marks (group-mark
                   (from data-name
                         [:stack :groupby [x] :sortby [g] :field y]
                         [:facet :groupby [g]])
                   :marks (marks [:area
                                  :properties (properties :enter [[:x :scale "x" :field x]
                                                                  [:y :scale "y" :field "layout_start"]
                                                                  [:y2 :scale "y" :field "layout_end"]
                                                                  [:interpolate :value "monotone"]
                                                                  [:fill :scale "color" :field g]
                                                                  [:font :value "Helvetica Neue"]])]))
           :scales (scales [:x :width
                            :type (if time? "time" "linear")
                            :zero false
                            :domain {:data data-name :field x}]
                           [:y :height
                            :nice true
                            :domain {:data "stats" :field (str "sum_" y)}]
                           [:color color
                            :type "ordinal"
                            :domain {:data data-name :field g}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))
