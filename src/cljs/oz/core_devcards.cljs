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

(defcard data-table-card
  "Simple data-table example"
  (devcards/reagent simple-data-table-example))

(defn ^:export main [] (devcards.core/start-devcard-ui!))

