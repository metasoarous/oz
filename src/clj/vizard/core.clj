(ns vizard.core
  (:require [vizard.server :as server]
            [vizard.vega :as vega]
            [vizard.plot :as p]
            [org.httpkit.client :as client]
            [cheshire.core :as json]))

(def start-plot-server! server/start!)

(defn plot! [spec & {:keys [host port]
                     :or {port (:port @server/web-server_ 10666)
                          host "localhost"}}]
  (client/post (str "http://" host ":" port "/spec")
               {:body (json/generate-string spec)})
  spec)

(comment
  (letfn [(group-data [& names]
            (apply concat (for [n names]
                            (map-indexed (fn [i x] {:foo i :bar x :biz n}) (take 20 (repeatedly #(rand-int 100)))))))]
    (plot! (p/vizard {:mark-type :area :x :foo :y :bar :g :biz :color "category20b" :legend? false} (group-data "foo" "bar" "baz" "poot")))))
