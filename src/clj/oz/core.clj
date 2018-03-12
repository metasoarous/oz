(ns oz.core
  (:require [oz.server :as server]
            [clj-http.client :as client]
            [aleph.http :as aleph]
            [clojure.string :as string]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
            [tentacles.gists :as gists]))
             

(def start-plot-server! ^{:doc "Start the oz plot server on localhost:10666 by default."}
  server/start!)

(defonce ^{:private true} cookie-store (clj-http.cookies/cookie-store))
(defonce ^{:private true} anti-forgery-token (atom nil))


(defn view!
  "More general view function which takes specs in hiccup form, where vega/vega-lite blocks can be
  passed as `[:vega-lite plot-data]` (e.g.), nested within arbitrary hiccup."
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

(defn gist-plot!
  "Create a gist with the given plot data."
  [plot & {:keys [name description public]
           :or {name "plot"
                description "vega/vega-lite plot; see github.com/metasoarous/oz"
                public false}}]
  (let [plot-json (json/generate-string plot)
        gist (gists/create-gist {name plot-json} {:description description :public public})]
    gist))

(defn- vega-editor-url
  [gist-url & {:keys [mode] :or {mode :vega-lite}}]
  (str
    "https://vega.github.io/editor/#/gist/"
    (name mode)
    "/"
    (-> gist-url (string/split #"\/") reverse (->> (take 2) reverse (string/join "/")))))

(defn publish-plot!
  "Publish plot via `gitst-plot!`, and print out a vega-editor url correspond to said gist."
  [plot & {:keys [data mode name] :or {mode :vega-lite name "plot"}}]
  (let [plot (if data (assoc plot :data data) plot)
        gist (gist-plot! plot :name name)
        gist-url (:url gist)]
    (println "Gist url:" gist-url)
    (println "Vega editor url:" (vega-editor-url gist-url :mode mode))))


