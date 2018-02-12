(ns oz.core
  (:require [oz.server :as server]
            [clj-http.client :as client]
            [aleph.http :as aleph]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]))

(def start-plot-server! ^{:doc "Start the oz plot server on localhost:10666 by default."}
  server/start!)

(defonce cookie-store (clj-http.cookies/cookie-store))
(defonce anti-forgery-token (atom nil))


(defn view!
  [spec & {:keys [data host port mode]
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
    (server/send-all! [::view-spec spec])
    (catch Exception e
      (errorf "error sending plot to server: %s" (ex-data e)))))

(defn v!
  "Take a vega or vega-lite clojure map `spec` and POST it to a oz
  server running at `:host` and `:port` to be rendered."
  [spec & {:keys [data host port mode]
           :or {port (:port @server/web-server_ 10666)
                host "localhost"
                mode :vega-lite}}]
  (let [spec (if data (assoc spec :data data) spec)]
    (view! [mode spec] :host host :port port)))

