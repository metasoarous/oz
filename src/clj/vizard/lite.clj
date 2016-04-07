(ns vizard.lite
  (:require [vizard.colors :refer [brews]]))

(defn colors [name]
  (get brews name name))

(defn lite [vl-spec data-vals]
  (assoc-in vl-spec [:data :values] data-vals))
