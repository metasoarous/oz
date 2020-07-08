(ns example.core
  (:require [cljsjs.vega-spec-injector :as vsi]))

(defn init
  [& args]
  (println "I got called!" args))
