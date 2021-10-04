(ns oz.core
  (:require ["vega-embed" :as vegaEmbed]
            ["vega" :as vega]
            ;["leaflet-vega" :as leafletVega]
            ;["leaflet" :as leaflet]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [reagent.dom :as rd]))



(enable-console-print!)


(defn- apply-log-level
  [{:as opts :keys [log-level]}]
  (if (or (keyword? log-level) (string? log-level))
    (-> opts
        (dissoc :log-level)
        (assoc :logLevel
               (case (keyword log-level)
                 :debug vega/Debug
                 :info vega/Info
                 :warn vega/Warn)))
    opts))

(defn ^:no-doc embed-vega
  ([elem doc] (embed-vega elem doc {}))
  ([elem doc {:as opts :keys [view-callback]}]
   (when doc
     (let [doc (clj->js doc)
           opts (-> opts
                    (dissoc :view-callback)
                    (->> (merge {:renderer :canvas
                                 :mode "vega-lite"}))
                    (apply-log-level))
           opts (merge {:renderer :canvas}
                        ;; Have to think about how we want the defaults here to behave
                       opts)]
       (-> (vegaEmbed elem doc (clj->js opts))
           (.then (fn [res]
                    (when view-callback
                      (view-callback (.-view res)))))
           (.catch (fn [err]
                     (js/console.log err))))))))

;; WIP; TODO Finish figuring this out; A little thornier than I thought, because data can come in so many
;; different shapes; Should clojure.spec this out:
;; * url
;; * named data
;; * vega vs lite
;; * data nested in layers
;; * other?
(defn ^:no-doc update-vega
  ([elem old-doc new-doc old-opts new-opts]
   (case
     ;; Only rerender from scratch if the viz specification has actually changed, or if always rerender is
     ;; specified
     (or (:always-rerender new-opts)
         (not= (dissoc old-doc :data) (dissoc new-doc :data))
         (not= old-opts new-opts))
     (embed-vega elem new-doc new-opts)
     ;; Otherwise, just update the data component
     ;; TODO This is the hard part to figure out
     ;(= ())
     ;()
     ;; Otherwise, do nothing
     :else
     nil)))

(defn vega
  "Reagent component that renders vega"
  ([doc] (vega doc {}))
  ([doc opts]
   ;; Is this the right way to do this? So vega component behaves abstractly like a vega-lite potentially?
   (let [opts (merge {:mode "vega"} opts)]
     (r/create-class
      {:display-name "vega"
       :component-did-mount (fn [this]
                              (embed-vega (rd/dom-node this) doc opts))
       ;; Need to look into this further to see how these args even work; may not be doing new-opts right here?
       ;; (http://reagent-project.github.io/docs/master/reagent.core.html)
       ;; (https://reactjs.org/docs/react-component.html#unsafe_componentwillupdate)
       :component-will-update (fn [this [_ new-doc new-opts]]
                                ;(update-vega (rd/dom-node this) doc new-doc opts new-opts)
                                (embed-vega (rd/dom-node this) new-doc new-opts))
       :reagent-render (fn [doc]
                         [:div.viz])}))))

(defn vega-lite
  "Reagent component that renders vega-lite."
  ([doc] (vega-lite doc {}))
  ([doc opts]
   ;; Which way should the merge go?
   (vega doc (merge opts {:mode "vega-lite"}))))


(def ^:private live-viewers-state
  (r/atom {:vega vega
           :vega-lite vega-lite}))

(defn register-live-view
  [key component]
  (swap! live-viewers-state assoc key component))

(defn register-live-views
  [& {:as live-views}]
  (swap! live-viewers-state merge live-views))


(def ^:no-doc default-data-table-opts
  {:per-page 50
   :tr-style {}
   :td-style {:padding-right 10}
   :th-style {:padding-right 10
              :cursor :pointer}})

(defn data-table
  ([data] (data-table data {}))
  ([data {:keys [page sort-key sort-order]}]
   (let [state (r/atom {:page (or page 0) :sort-key sort-key :sort-order (or sort-order :ascending)})
         header (->> data (take 10) (map (comp set keys)) (reduce clojure.set/union))]
     (fn [data opts]
       (let [{:keys [page sort-key sort-order]} @state
             {:keys [per-page tr-style td-style th-style]}
             (merge-with (fn [opt1 opt2]
                           (if (and (map? opt1) (map? opt2))
                             (merge opt1 opt2)
                             opt2))
               default-data-table-opts
               opts)
             scoped-data (cond->> data
                           sort-key (sort-by sort-key)
                           (= :descending sort-order) (reverse)
                           per-page (drop (* per-page page))
                           per-page (take per-page))
             last-page (quot (count data) per-page)]
         [:div
          (when (> (count data) per-page)
            [:p
             {:style {:margin-bottom 10}}
             [:span
              {:style {:padding-right 20}}
              "Current page: " (inc page)]
             (when (> page 0)
               [:a
                {:on-click (fn [& _] (swap! state update :page dec))
                 :style {:padding-right 10
                         :cursor :pointer}}
                "prev"])
             (when (< page last-page)
               [:a
                {:on-click (fn [& _] (swap! state update :page inc))
                 :style {:padding-right 10
                         :cursor :pointer}}
                "next"])])
          [:table
           ;; header row
           [:tr
            {:style tr-style}
            (for [key header]
              ^{:key key}
              [:th {:style th-style
                    :on-click (fn [& _]
                                (swap! state merge {:sort-key key
                                                    :sort-order (if (and (= key sort-key) (= sort-order :ascending))
                                                                  :descending
                                                                  :ascending)}))}
                (name key)
                (when (= sort-key key)
                  (case sort-order
                    :ascending "⌃"
                    :descending "⌄"))])]
           (for [row scoped-data]
             ^{:key (hash row)}
             [:tr
              {:style tr-style}
              (for [key header]
                ^{:key key}
                [:td {:style td-style} (get row key)])])]])))))


(register-live-views
  :vega vega
  :vega-lite vega-lite
  :data-table data-table)

(defn ^:no-doc live-view
  ;; should handle sharing data with nodes that need it?
  [doc]
  ;; prewalk spec, rendering special hiccup tags like :vega and :vega-lite, and potentially other composites,
  ;; rendering using the components above. Leave regular hiccup unchanged).
  ;; TODO finish writing; already hooked in below so will break now
  (let [live-viewers @live-viewers-state
        live-viewer-keys (set (keys live-viewers))]
    (clojure.walk/prewalk
      (fn [x] (if (and (coll? x) (live-viewer-keys (first x)))
                (into
                  [(get live-viewers (first x))]
                  (rest x))
                x))
      doc)))


;; TODO Rename this to live-view; But need to make sure to edit in the repl tooling application code as well,
;; since that's what actually uses this
(def ^:no-doc view-spec live-view)
  ;; should handle sharing data with nodes that need it?



;(comment)
  ;; This is still a work in progress
  ;(defn ^:private render-leaflet-vega [dom-node]
    ;;(.map leaflet dom-node)
    ;(let [m (.map leaflet "map")
          ;_ (.setView m (clj->js [51.505 -0.09]) 4)
          ;tile (.tileLayer leaflet
                           ;"https://maps.wikimedia.org/osm-intl/{z}/{x}/{y}.png"
                           ;(clj->js {:attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}))

          ;_ (.addTo tile m)
          ;marker (.marker leaflet (clj->js [40.7128 -74.0059]))]
      ;;(js/console.log (clj->js [40.7128 -74.0059]))
      ;(.addTo marker m)))
      ;;(.bindPopup marker "a red-headed rhino")))

  ;;; This is still a work in progress
  ;(defn ^:private leaflet-vega
    ;"WIP/Alpha wrapper around leaflet-vega"
    ;[]
    ;(r/create-class
      ;{:display-name "leaflet-vega"
       ;:component-did-mount (fn [this]
                              ;(render-leaflet-vega (rd/dom-node this)))
       ;:component-did-update (fn [this [_]]
                               ;(render-leaflet-vega (rd/dom-node this)))
       ;:reagent-render (fn []
                         ;[:div#map])})))


