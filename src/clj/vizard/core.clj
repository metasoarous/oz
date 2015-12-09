(ns vizard.core
  (:require [vizard.server :as server]
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
  (defn group-data [& names]
    (apply concat (for [n names]
                    (map-indexed (fn [i x] {:x i :y x :col n}) (take 20 (repeatedly #(rand-int 100)))))))
  (plot! (p/vizard {:mark-type :bar
                    :encoding {:x {:field :x :scale :ordinal}
                               :y {:field :y :scale :linear}
                               :g {:field :col}}
                    :color "category20b"
                    :legend? true}
                   (group-data "foo" "bar" "baz" "poot")))
  (plot! (p/vizard {:mark-type :line
                    :encoding {:x {:field :x :scale :linear}
                               :y {:field :y :scale :linear}
                               :g {:field :col}}
                    :color "category20b"
                    :legend? true}
                   (group-data "foo" "bar" "baz" "poot"))))
