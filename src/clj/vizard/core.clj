(ns vizard.core
  (:require [vizard.server :as server]
            [vizard.plot :as p]
            [aleph.http :as aleph]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(def ^{:doc "start the vizard plot server on localhost:10666 by default."}
  start-plot-server! server/start!)

(defn p!
  "Take a vega-lite clojure map `spec` and POST it to a vizard
  server running at `:host` and `:port` to be rendered."
  [spec & {:keys [host port]
           :or {port (:port @server/web-server_ 10666)
                host "localhost"}}]
  @(aleph/post (str "http://" host ":" port "/vl-spec")
               {:body (json/generate-string spec)})
  spec)

(defn plot!
  "Take a vizard clojure map `spec` and POST it to a vizard
  server running at `:host` and `:port` to be rendered."
  [spec & {:keys [host port]
           :or {port (:port @server/web-server_ 10666)
                host "localhost"}}]
  (let [resp (-> @(aleph/post (str "http://" host ":" port "/spec")
                           {:body (json/generate-string spec)}))]
    (debugf "server response: %s" resp)
    spec))

(defn to-json
  "Take a vizard or vega-lite clojure map `spec` and convert it to json for debugging
  or usage in the vega editor.

  Ex. `(println (to-json spec))`"
  [spec]
  (json/generate-string spec))

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
                   (group-data "foo" "bar" "baz" "poot")))

  (defn heat-data [w h]
    (for [x (range w)
          y (range h)]
      {:x x :y y :z (rand)}))

  (plot! (p/vizard {:mark-type :heatmap
                    :encoding {:x {:field :x :scale :ordinal}
                               :y {:field :y :scale :ordinal}
                               :z {:field :z}}
                    :legend? true}
                   (heat-data 20 20))))
