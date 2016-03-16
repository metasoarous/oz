(ns vizard.plot
  (:refer-clojure :exclude [conj])
  (:require [cheshire.core :as json]
            [com.rpl.specter :as s]
            [vizard.histogram :as h]
            [vizard.colors :refer [brews]]))

;; colors

(defn colors [name]
  (get brews name name))

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

(defn vega
  [& args]
  (let [opts (apply hash-map args)]
    (merge {:height 500 :width 960} opts)))

;; config

(defn x-scale [encoding]
  (or
   (keyword (first (s/select [:x :scale] encoding)))
   :linear))

(defn x-field [encoding]
  (or
   (keyword (first (s/select [:x :field] encoding)))
   :x))

(defn y-scale [encoding]
  (or
   (keyword (first (s/select [:y :scale] encoding)))
   :linear))

(defn y-field [encoding]
  (or
   (keyword (first (s/select [:y :field] encoding)))
   :y))

(defn z-field [encoding]
  (or
   (keyword (first (s/select [:z :field] encoding)))
   :z))

(defn group-field [encoding]
  (or
   (keyword (first (s/select [:g :field] encoding)))
   :col))

(defn time? [encoding d]
  (concat d (when (= (x-scale encoding) "time") [:format {:parse {(x-field encoding) "date"}}])))

(defn x-label [encoding]
  (keyword (first (s/select [:x :label] encoding))))

(defn y-label [encoding]
  (keyword (first (s/select [:y :label] encoding))))


;; update stuff

(defn update-data [aname key val spec]
  (s/setval [:data s/ALL #(= (keyword (:name %)) aname) key] val spec))

(defn update-scale [aname key val spec]
  (s/setval [:scales s/ALL #(= (keyword (:name %)) aname) key] val spec))

(defn update-axis [atype key val spec]
  (s/setval [:axes s/ALL #(= (keyword (:type %)) atype) key] val spec))

(defn conj [key val spec]
  (s/setval [key s/END] [val] spec))

;; plots

(defmulti vizard (fn [config _]
                   (:mark-type config)))

(defmethod vizard :line
  [config data-vals]
  (let [{:keys [mark-type legend? color encoding]
         :or {legend? true
              color "category20"
              encoding {:x {:field "x" :scale "linear"}
                        :y {:field "y" :scale "linear"}
                        :g {:field "col"}}}} config
        x (x-field encoding)
        y (y-field encoding)
        xscale (x-scale encoding)
        yscale (y-scale encoding)
        g (group-field encoding)
        xlabel (x-label encoding)
        ylabel (y-label encoding)
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals])
           :axes (axes [:x "x" :title xlabel] [:y "y" :title ylabel])
           :marks (group-mark
                   (from data-name [:facet :groupby [g]])
                   :marks (marks [:line
                                  :properties (properties :enter [[:x :scale "x" :field x]
                                                                  [:y :scale "y" :field y]
                                                                  [:stroke :scale "color" :field g]
                                                                  [:strokeWidth :value 2]])]))
           :scales (scales [:x :width
                            :type xscale
                            :domain {:data data-name :field x}]
                           [:y :height
                            :type yscale
                            :nice true
                            :domain {:data data-name :field y}]
                           [:color (colors color)
                            :type "ordinal"
                            :domain {:data data-name :field g}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :scatter
  [config data-vals]
  (let [{:keys [mark-type legend? color encoding]
         :or {legend? true
              color "category20"
              encoding {:x {:field "x" :scale "linear"}
                        :y {:field "y" :scale "linear"}
                        :g {:field "col"}}}} config
        x (x-field encoding)
        y (y-field encoding)
        xscale (x-scale encoding)
        yscale (y-scale encoding)
        g (group-field encoding)
        xlabel (x-label encoding)
        ylabel (y-label encoding)
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals])
           :axes (axes [:x "x" :title xlabel] [:y "y" :title ylabel])
           :marks (group-mark
                   (from data-name [:facet :groupby [g]])
                   :marks (marks [:symbol
                                  :properties (properties :enter [[:x :scale "x" :field x]
                                                                  [:y :scale "y" :field y]
                                                                  [:size :value 100]
                                                                  [:fill :scale "color" :field g]])]))
           :scales (scales [:x :width
                            :type xscale
                            :domain {:data data-name :field x}]
                           [:y :height
                            :type yscale
                            :nice true
                            :domain {:data data-name :field y}]
                           [:color (colors color)
                            :type "ordinal"
                            :domain {:data data-name :field g}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :area
  [config data-vals]
  (let [{:keys [mark-type legend? color encoding]
         :or {legend? true
              color "category20"
              encoding {:x {:field "x" :scale "linear"}
                        :y {:field "y" :scale "linear"}
                        :g {:field "col"}}}} config
        x (x-field encoding)
        y (y-field encoding)
        xscale (x-scale encoding)
        yscale (y-scale encoding)
        g (group-field encoding)
        xlabel (x-label encoding)
        ylabel (y-label encoding)
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals]
                       [:stats
                        :source data-name
                        :transform (transforms [:aggregate
                                                :groupby [x]
                                                :summarize [{:field y :ops ["sum"]}]])])
           :axes (axes [:x "x" :title xlabel] [:y "y" :title ylabel])
           :marks (group-mark
                   (from data-name
                         [:stack :groupby [x] :sortby [g] :field y]
                         [:facet :groupby [g]])
                   :marks (marks [:area
                                  :properties (properties :enter [[:x :scale "x" :field x]
                                                                  [:y :scale "y" :field "layout_start"]
                                                                  [:y2 :scale "y" :field "layout_end"]
                                                                  [:interpolate :value "monotone"]
                                                                  [:fill :scale "color" :field g]])]))
           :scales (scales [:x :width
                            :type xscale
                            :points true
                            :domain {:data data-name :field x}]
                           [:y :height
                            :type yscale
                            :nice true
                            :domain {:data "stats" :field (str "sum_" (name y))}]
                           [:color (colors color)
                            :type "ordinal"
                            :domain {:data data-name :field g}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :bar
  [config data-vals]
  (let [{:keys [mark-type legend? color encoding]
         :or {legend? true
              color "category20"
              encoding {:x {:field "x" :scale "ordinal"}
                        :y {:field "y" :scale "linear"}
                        :g {:field "col"}}}} config
        x (x-field encoding)
        y (y-field encoding)
        xscale (x-scale encoding)
        yscale (y-scale encoding)
        g (group-field encoding)
        xlabel (x-label encoding)
        ylabel (y-label encoding)
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals]
                       [:stats
                        :source data-name
                        :transform (transforms [:aggregate
                                                :groupby [x]
                                                :summarize [{:field y :ops ["sum"]}]])])
           :axes (axes [:x "x" :title xlabel] [:y "y" :title ylabel])
           :marks (marks [:rect
                          :from (from data-name [:stack :groupby [x] :sortby [g] :field y])
                          :properties (properties :enter [[:x :scale "x" :field x]
                                                          [:width :scale "x" :band true :offset -1]
                                                          [:y :scale "y" :field "layout_start"]
                                                          [:y2 :scale "y" :field "layout_end"]
                                                          [:fill :scale "color" :field g]])])
           :scales (scales [:x :width
                            :type xscale
                            :domain {:data data-name :field x}]
                           [:y :height
                            :type yscale
                            :nice true
                            :domain {:data "stats" :field (str "sum_" (name y))}]
                           [:color (colors color)
                            :type "ordinal"
                            :domain {:data data-name :field g}])
           :padding "auto")]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :grouped-bar
  [config data-vals]
  (let [{:keys [mark-type legend? color encoding]
         :or {legend? true
              color "category20"
              encoding {:x {:field "x" :scale "linear"}
                        :y {:field "y" :scale "linear"}
                        :g {:field "col"}}}} config
        x (x-field encoding)
        y (y-field encoding)
        xscale (x-scale encoding)
        yscale (y-scale encoding)
        g (group-field encoding)
        xlabel (x-label encoding)
        ylabel (y-label encoding)
        data-name mark-type
        v (vega
           :data (data [data-name :values data-vals])
           :axes (axes [:x "x" :title xlabel] [:y "y" :title ylabel])
           :marks (group-mark
                   (from data-name
                         [:facet :groupby [x]])
                   :properties (properties :enter [[:x :scale "x" :field "key"]
                                                   [:width :scale "x" :band true]])
                   :marks (marks [:rect
                                  :properties (properties :enter [[:x :scale "pos" :field g]
                                                                  [:y :scale "y" :field y]
                                                                  [:y2 :scale "y" :value 0]
                                                                  [:width :scale "pos" :band true]
                                                                  [:fill :scale "color" :field g]])])
                   :scales (scales [:pos :width
                                    :type "ordinal"
                                    :domain {:data data-name :field g}]))
           :scales (scales [:x :width
                            :type xscale
                            :padding 0.2
                            :domain {:data data-name :field x}]
                           [:y :height
                            :type yscale
                            :nice true
                            :domain {:data data-name :field y}]
                           [:color (colors color)
                            :type "ordinal"
                            :domain {:data data-name :field g}]))]
    (if legend?
      (assoc v :legends (legends [:fill "color"]))
      v)))

(defmethod vizard :heatmap
  [config data-vals]
  (let [{:keys [mark-type legend? color encoding]
         :or {legend? true
              color "category20"
              encoding {:x {:field "x" :scale "ordinal"}
                        :y {:field "y" :scale "ordinal"}
                        :z {:field "z"}
                        :g {:field "col"}}}} config
        x (x-field encoding)
        y (y-field encoding)
        z (z-field encoding)
        xscale (x-scale encoding)
        yscale (y-scale encoding)
        g (group-field encoding)
        xlabel (x-label encoding)
        ylabel (y-label encoding)
        data-name mark-type
        hist (h/uniform (map :z data-vals) 10)
        {:keys [xmin xmax]} hist
        edges (map double (h/edges hist))
        xmedian (nth edges 5)
        v (vega
           :data (data [data-name :values data-vals])
           :axes (axes [:x "x" :title xlabel] [:y "y" :title ylabel])
           :marks (marks [:rect
                          :from (from data-name)
                          :properties (properties :enter [[:x :scale "x" :field x]
                                                          [:width :scale "x" :band true]
                                                          [:y :scale "y" :field y]
                                                          [:height :scale "y" :band true]
                                                          [:fill :scale "color" :field z]])])
           :scales (scales [:x :width
                            :type xscale
                            :domain {:data data-name :field x}]
                           [:y :height
                            :type yscale
                            :nice true
                            :domain {:data data-name :field y}]
                           [:color (reverse (colors "RdYlBu"))
                            :type "linear"
                            :domain (vec edges)
                            :zero false])
           :padding "auto")]
    (if legend?
      (assoc v :legends (legends [:fill "color" :values [xmin xmedian xmax]]))
      v)))
