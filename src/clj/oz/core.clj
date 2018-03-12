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

(def ^{:private true} vega-spec-opts
  #{:data :width :height :datasets})

;; For right now, leaving this private, since really implementation detail to main repl api functions below.
;; But eventually, would be nice to rename this and add a public merge-opts which just does the remove nil
;; step, as a helper basically.
(defn- merge-opts
  "Merge relevant api opts into vega data structure, removing entries with nil values"
  [spec opts]
  (->> opts
       (filter (comp vega-spec-opts first))
       (remove (comp nil? second))
       (into spec)))

(defn v!
  "Take a vega or vega-lite clojure map `spec` and POST it to a oz
  server running at `:host` and `:port` to be rendered."
  [spec & {:as opts
           :keys [data width height host port mode]
           :or {port (:port @server/web-server_ 10666)
                host "localhost"
                mode :vega-lite}}]
  ;; Update spec opts, then send view
  (let [spec (merge-opts spec opts)]
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
  [plot & {:as opts
           :keys [data width height mode name]
           :or {mode :vega-lite name "plot"}}]
  (let [plot (merge-opts plot opts)
        gist (gist-plot! plot :name name)
        gist-url (:url gist)]
    (println "Gist url:" gist-url)
    (println "Vega editor url:" (vega-editor-url gist-url :mode mode))))


