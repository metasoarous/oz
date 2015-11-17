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

(defn mk-data [aname values]
  {:name (name aname)
   :values values})

(defn vega
  [data & args]
  (let [opts (apply hash-map args)]
    (merge {:data data :height 500 :width 960} opts)))

;; plots

(defmulti vizard (fn [config _]
                   (:mark-type config)))

(defmethod vizard :line
  [config data-vals]
  (let [{:keys [mark-type group-key time? legend? color]
         :or {group-key "col"
              time? false
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
           [(mk-data mark-type data-vals)]
           :axes (axes [:x "x"] [:y "y"])
           :marks (group-mark
                   (from data-name [:facet :groupby [group-key]])
                   :marks (marks [:line
                                  :properties (properties :enter [[:x :scale "x" :field "x"]
                                                                  [:y :scale "y" :field "y"]
                                                                  [:stroke :scale "color" :field group-key]
                                                                  [:strokeWidth :value 2]])]))
           :scales (scales [:x :width
                            :type (if time? "time" "linear")
                            :domain {:data data-name :field "x"}]
                           [:y :height
                            :nice true
                            :domain {:data data-name :field "y"}]
                           [:color color
                            :type "ordinal"
                            :domain {:data data-name :field group-key}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :scatter
  [config data-vals]
  (let [{:keys [mark-type group-key time? legend? color]
         :or {group-key "col"
              time? false
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
           [(mk-data mark-type data-vals)]
           :axes (axes [:x "x"] [:y "y"])
           :marks (group-mark
                   (from data-name [:facet :groupby [group-key]])
                   :marks (marks [:symbol
                                  :properties (properties :enter [[:x :scale "x" :field "x"]
                                                                  [:y :scale "y" :field "y"]
                                                                  [:size :value 100]
                                                                  [:fill :scale "color" :field group-key]])]))
           :scales (scales [:x :width
                            :type (if time? "time" "linear")
                            :domain {:data data-name :field "x"}]
                           [:y :height
                            :nice true
                            :domain {:data data-name :field "sum_y"}]
                           [:color color
                            :type "ordinal"
                            :domain {:data data-name :field group-key}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :bar
  [config data-vals]
  (let [{:keys [mark-type group-key legend? color]
         :or {group-key "col"
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
     [(mk-data data-name data-vals)
      {:name "stats"
       :source (name data-name)
       ;; TODO: make this not ugly
       :transform (transforms [:aggregate
                               :groupby ["x"]
                               :summarize [{:field "y" :ops ["sum"]}]])}]
     :axes (axes [:x "x"] [:y "y"])
     :marks (marks [:rect
                    :from (from data-name [:stack :groupby ["x"] :sortby [group-key] :field "y"])
                    :properties (properties :enter [[:x :scale "x" :field "x"]
                                                    [:width :scale "x" :band true :offset -1]
                                                    [:y :scale "y" :field "layout_start"]
                                                    [:y2 :scale "y" :field "layout_end"]
                                                    [:fill :scale "color" :field group-key]]
                                            :update [[:fillOpacity :value 1.0]]
                                            :hover [[:fillOpacity :value 0.5]])])
     :scales (scales [:x :width :type "ordinal"
                      :domain {:data data-name :field "x"}]
                     [:y :height :type "linear" :nice true
                      :domain {:data "stats" :field "sum_y"}]
                     [:color color
                      :type "ordinal"
                      :domain {:data data-name :field group-key}])
     :padding "auto")]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :area
  [config data-vals]
  (let [{:keys [mark-type group-key time? legend? color]
         :or {group-key "col"
              time? false
              legend? true
              color "category20"}} config
        data-name mark-type
        v (vega
           [(mk-data mark-type data-vals)
            {:name "stats"
             :source (name data-name)
             ;; TODO: make this not ugly
             :transform (transforms [:aggregate
                                     :groupby ["x"]
                                     :summarize [{:field "y" :ops ["sum"]}]])}]
           :axes (axes [:x "x"] [:y "y"])
           :marks (group-mark
                   (from data-name
                         [:stack :groupby ["x"] :sortby [group-key] :field "y"]
                         [:facet :groupby [group-key]])
                   :marks (marks [:area
                                  :properties (properties :enter [[:x :scale "x" :field "x"]
                                                                  [:y :scale "y" :field "layout_start"]
                                                                  [:y2 :scale "y" :field "layout_end"]
                                                                  [:interpolate :value "monotone"]
                                                                  [:fill :scale "color" :field group-key]])]))
           :scales (scales [:x :width
                            :type (if time? "time" "linear")
                            :zero false
                            :domain {:data data-name :field "x"}]
                           [:y :height
                            :nice true
                            :domain {:data "stats" :field "sum_y"}]
                           [:color color
                            :type "ordinal"
                            :domain {:data data-name :field group-key}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))
