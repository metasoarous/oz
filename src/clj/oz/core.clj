(ns oz.core
  (:require [oz.server :as server]
            [clj-http.client :as client]
            [aleph.http :as aleph]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
            [tentacles.gists :as gists]))


;; Utils

(defn- mapply
  "utility function for applying kw-args"
  [f & args]
  (apply f (concat (butlast args) (flatten (into [] (last args))))))

(defn- spec-type [spec]
  (if (sequential? spec) :ozviz :vega))

(def ^{:private true} vega-spec-opts
  #{:data :width :height :datasets})

(defn- merge-opts
  "Merge relevant api opts into vega data structure, removing entries with nil values"
  [spec opts]
  (->> opts
       (filter (comp vega-spec-opts first))
       (remove (comp nil? second))
       (into spec)))

(defn- submap
  [m keys]
  (into {} (filter #((set keys) (first %)) m)))


;; Set up plot server crap

(def start-plot-server! ^{:doc "Start the oz plot server on localhost:10666 by default."}
  server/start!)

(defonce ^{:private true} cookie-store (clj-http.cookies/cookie-store))
(defonce ^{:private true} anti-forgery-token (atom nil))

(defn- prepare-server-for-view!
  [port host]
  ;; start the webserver if needed
  (when-not (server/web-server-started?)
    (infof "Starting up server on port" port)
    (start-plot-server! port)
    (Thread/sleep 7500))
  (when-not @anti-forgery-token
    (when-let [token (:csrf-token
                      (json/parse-string
                       (:body (client/get (str "http://" host ":" port "/token")
                                          {:cookie-store cookie-store}))
                       keyword))]
      (reset! anti-forgery-token token))))


;; Main view functions

(defn view!
  "More general view function which takes specs in hiccup form, where vega/vega-lite blocks can be
  passed as `[:vega-lite plot-data]` (e.g.), nested within arbitrary hiccup."
  [spec & {:keys [data host port]
           :or {port (:port @server/web-server_ 10666)
                host "localhost"}}]
  (try
    (prepare-server-for-view! port host)
    (server/send-all! [::view-spec spec])
    (catch Exception e
      (errorf "error sending plot to server: %s" (ex-data e)))))


(defn v!
  "Take a vega or vega-lite clojure map `spec` and POST it to an oz
  server running at `:host` and `:port` to be rendered."
  [spec & {:as opts
           :keys [data width height host port mode]
           :or {port (:port @server/web-server_ 10666)
                host "localhost"
                mode :vega-lite}}]
  ;; Update spec opts, then send view
  (let [spec (merge-opts spec opts)]
    (view! [mode spec] :host host :port port)))



;; Publishing code

(defn- auth-args
  [args]
  (let [the-auth-args (submap args #{:auth :auth-token :client-id :access-token})
        auth-file (or (:auth-file args) (str (System/getProperty "user.home") "/.oz/github-creds.edn"))]
    (if (empty? the-auth-args)
      (try
        (edn/read-string (slurp auth-file))
        (catch Exception e
          (errorf "Unable to find/parse github authorization file `~/.oz/github-creds.edn`. Please review the output of `(doc oz/publish!)` for auth instructions.")
          (throw e)))
      the-auth-args)))

(defn gist!
  "Create a gist with the given spec.

  Requires authentication, which must be provided by one of the following opts:
  * `:auth`: a Github auth token the form \"username:password\"
  * `:auth-token`: a GitHub OAuth1 / Personal access token as a string (recommended)
  * for oauth2:
    * `:client-id`: an oauth2 client id property
    * `:access-token`: oauth2 access token
  
  CAUTION: Note that running these options from the REPL may leave sensitive data in your `./.lein-repl-history` file.
  Thus it's best that you avoid using these options, and instead create a single edn file at `~/.oz/github-creds.edn` with these opts.
  You can run `chmod 600` on it, so that only the owner is able to access it.
  If you want to specify a different path use:
  * `:auth-file`: defaults to `~/.oz/github-creds.edn`.
  
  Additional options:
  * `:public`: default false
  * `:description`: auto generated based on spec"
  [spec & {:as opts
           :keys [name description public]
           :or {public false}}]
  (let [type (spec-type spec)
        name (or name
               (case type
                 :ozviz "ozviz-document.edn"
                 :vega "vega-viz.json"))
        description (or description
                      (case type
                        :ozviz "Ozviz document; To load go to https://ozviz.io/#/gist/<gist-id>."
                        :vega "Vega/Vega-Lite viz; To load go to https://vega.github.io/editor"))
        spec-string (case type
                      :ozviz (pr-str spec)
                      :vega (json/generate-string spec))
        create-gist-opts (merge {:description description :public public}
                                (auth-args opts))
        gist (gists/create-gist {name spec-string} create-gist-opts)]
    gist))

;; Testing out
;(try (gist! [:this "stuff"])
     ;(catch Exception e (.printStackTrace e)))


(defn- vega-editor-url
  [{:as gist :keys [owner id history files]} & {:keys [mode] :or {mode :vega-lite}}]
  (str
    "https://vega.github.io/editor/#/gist/"
    (string/join "/"
      [(name mode) (:login owner) id (-> history first :version) (-> gist :files first second :filename)])))

(defn- ozviz-url
  [gist-url]
  (str
    "http://ozviz.io/#/gist/"
    (->> gist-url (re-find #"\/gists\/?(.*)") second)))


(defn publish!
  "Publish spec via gist! and print out the corresponding vega-editor or ozviz.io url.

  Requires authentication, which must be provided by one of the following opts:
  * `:auth`: a Github auth token the form \"username:password\"
  * `:oauth-token`: a GitHub OAuth1 / Personal access token as a string (recommended)
  * for oauth2:
    * `:client-id`: an oauth2 client id property
    * `:access-token`: oauth2 access token
  
  CAUTION: Note that running these options from the REPL may leave sensitive data in your `./.lein-repl-history` file.
  Thus it's best that you avoid using these options, and instead create a single edn file at `~/.oz/github-creds.edn` with these opts.
  You can run `chmod 600` on it, so that only the owner is able to access it.
  If you want to specify a different path use:
  * `:auth-file`: defaults to `~/.oz/github-creds.edn`.
  
  Additional options:
  * `:public`: default false
  * `:description`: auto generated based on spec
  * `:return-full-gist`: return the full tentacles gist api response data"
  [spec & {:as opts
           :keys [mode return-full-gist]
           :or {mode :vega-lite}}]
  (let [gist (mapply gist! spec opts)
        gist-url (:url gist)]
    (println "Gist url:" (:html_url gist))
    (println "Raw gist url:" gist-url)
    ;; Should really merge these into gist and return as data...
    (case (spec-type spec)
      :ozviz (println "Ozviz url:" (ozviz-url gist-url))
      :vega (println "Vega editor url:" (vega-editor-url gist :mode mode)))
    (when return-full-gist
      gist)))

(defn publish-plot!
  "Deprecated form of `publish!`"
  [plot & opts]
  (warnf "WARNING!!! DEPRECATED!!! Please call `publish!` instead.")
  (let [spec (merge-opts plot opts)]
    (publish! spec opts)))


