(ns vizard.lite
  (:require [vizard.colors :refer [brews]]
            [schema.core :as s]
            [vizard.schema :refer [Vega]]))

(defn colors [name]
  (get brews name name))

(defn lite [vl-spec data-vals]
  (assoc-in (s/validate Vega vl-spec) [:data :values] data-vals))
