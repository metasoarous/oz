(ns vizard.core
  (:require [vizard.server :as server]
            [clj-http.client :as client]
            [aleph.http :as aleph]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(def start-plot-server! ^{:doc "Start the vizard plot server on localhost:10666 by default."}
  server/start!)

(def cookie-store (clj-http.cookies/cookie-store))
(def anti-forgery-token (atom nil))

(defn p!
  "Take a vega-lite clojure map `spec` and POST it to a vizard
  server running at `:host` and `:port` to be rendered."
  [spec & {:keys [host port]
           :or {port (:port @server/web-server_ 10666)
                host "localhost"}}]
  (try
    (when-not @anti-forgery-token
      (when-let [token (:csrf-token
                        (json/parse-string
                         (:body (client/get (str "http://" host ":" port "/token")
                                            {:cookie-store cookie-store}))
                         keyword))]
        (reset! anti-forgery-token token)))
    (let [resp (client/post (str "http://" host ":" port "/vl-spec")
                            {:cookie-store cookie-store
                             :headers {"X-CSRF-Token" @anti-forgery-token}
                             :body (json/generate-string spec)})]
      (debugf "server response: %s" resp)
      spec)
    (catch Exception e
      (errorf "error sending plot to server: %s" (ex-data e)))))

(defn last-vl-spec
  "Returns the most recent vega spec sent to the vizard server."
  [& {:keys [host port]
      :or {port (:port @server/web-server_ 10666)
           host "localhost"}}]
  (try
    (json/parse-string (:body (client/get (str "http://" host ":" port "/vl-spec"))) keyword)
    (catch Exception e (errorf "error sending plot to server: %s" (ex-data e)))))

(defn to-json
  "Take a vizard or vega-lite clojure map `spec` and convert it to json for debugging
  or usage in the vega editor.

  Ex. `(println (to-json spec))`"
  [spec]
  (json/generate-string spec))
