(ns ^:no-doc oz.core-devcards
  (:require
    [cljsjs.react]
    [cljsjs.react.dom]
    [oz.core :as oz]
    [reagent.core :as r :include-macros true]
    [devcards.core :as devcards :include-macros true :refer [defcard]]))

(def sample-data
  [{:power 1 :speed 2 :engine :v8}
   {:power 3 :speed 4 :engine :v6}
   {:power 8 :speed 7 :engine :v8}
   {:power 4 :speed 3.5 :engine :v8}
   {:power 5 :speed 4 :engine :v8}
   {:power 1.2 :speed 3 :engine :v6}
   {:power 3.2 :speed 3.8 :engine :v6}
   {:power 7.5 :speed 7.2 :engine :v6}])

(defn simple-vega-lite-example []
  [oz/vega-lite
   {:data {:values sample-data}
    :mark {:type :point
           :tooltip true}
    :width 500
    :height 400
    :encoding {:x {:field :power}
               :y {:field :speed}
               :color {:field :engine}
               :size {:value 80}}}])

(defn log-level-example []
  [oz/vega-lite
   {:data {:values sample-data}
    :mark {:type :point
           :tooltip true}
    :transform [{:calculate "info(datum.speed/datum.power)"
                 :as "sp-ratio"}]
    :width 500
    :height 400
    :encoding {:x {:field :power}
               :y {:field :speed}
               :color {:field :engine}
               :size {:value 80}}}
   {:log-level :debug}])

(defn view-callback-example []
  [oz/vega-lite
   {:data {:values sample-data}
    :mark {:type :point
           :tooltip true}
    :width 500
    :height 400
    :encoding {:x {:field :power}
               :y {:field :speed}
               :color {:field :engine}
               :size {:value 80}}}
   {:view-callback
    (fn [view]
      (js/console.log "executing view-callback option to oz/vega-lite component")
      (js/console.log "view object" view))}])


(defn simple-data-table-example []
  [oz/data-table
   sample-data
   {:per-page 5}])

(defn simple-form2-component []
  (let [state (r/atom 0)]
    (fn []
      [:div
       [:a {:on-click (fn [& _] (swap! state inc))}
        "Click me"]
       [:h1 @state]])))

(defcard vega-lite-card
  "Simple vega-lite example"
  (devcards/reagent simple-vega-lite-example))

(defcard log-level-card
  "vega-lite spec with log-level set"
  (devcards/reagent log-level-example))

(defcard view-callback-example-card
  "vega-lite visualization executing a callback with the view object (check console log for logging result)"
  (devcards/reagent view-callback-example))

(defcard data-table-card
  "Simple data-table example"
  (devcards/reagent simple-data-table-example))

(defn ^:export main [] (devcards.core/start-devcard-ui!))

