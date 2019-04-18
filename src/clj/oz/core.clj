(ns oz.core
  (:refer-clojure :exclude [load])
  (:require [oz.server :as server]
            [oz.live :as live]
            [clj-http.client :as client]
            [aleph.http :as aleph]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [cheshire.core :as json]
            [yaml.core :as yaml]
            [markdown-to-hiccup.core :as markdown]
            [hiccup.core :as hiccup]
            [taoensso.timbre :as log :refer (tracef debugf infof warnf errorf)]
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

(defmacro ^:no-doc clone-var
  "Clone the var pointed to by fsym into current ns such that arglists, name and doc metadata are preserned."
  [fsym]
  (let [v (resolve fsym)
        m (submap (meta v) [:arglists :name :doc])
        m (update m :arglists (fn [arglists] (list 'quote arglists)))]
    `(def ~(vary-meta (:name m) (constantly m)) ~fsym)))


;; Set up plot server crap

;; Defines out function for manually starting the plot server
(clone-var server/start-plot-server!)

(defonce ^{:private true} cookie-store (clj-http.cookies/cookie-store))
(defonce ^{:private true} anti-forgery-token (atom nil))

(defn- prepare-server-for-view!
  [port host]
  ;; start the webserver if needed
  (infof "preparing: %s:%s" host port)
  (when (or (not= (server/get-server-port) port)
            (not (server/web-server-started?)))
    (infof "Starting up server on port: %s" port)
    (start-plot-server! port))
  (when-not @anti-forgery-token
    (when-let [token (:csrf-token
                      (json/parse-string
                       (:body (client/get (str "http://" host ":" port "/token")
                                          {:cookie-store cookie-store}))
                       keyword))]
      (reset! anti-forgery-token token))))


;; Main view functions

(defn view!
  "View the given spec in a web browser. Specs for which map? is true are treated as single Vega-Lite/Vega specifications.
  All other values are treated as hiccup, and are therefore expected to be a vector or other iterable.
  This hiccup may contain Vega-Lite/Vega visualizations embedded like `[:vega-lite spec]` or `[:vega spec]`.
  You may also specify `:host` and `:port`, for server settings, and a `:mode` option, defaulting to `:vega-lite`, with `:vega` the alternate option.
  (Though I will note that Vega-Embed often catches when you pass a vega spec to a vega-lite component, and does the right thing with it.
  However, this is not guaranteed behavior, so best not to depend on it (wink, nod))"
  [spec & {:keys [host port mode]}]
  (try
    (prepare-server-for-view! (or port (server/get-server-port) server/default-port) (or host "localhost"))
    (server/send-all!
      [::view-spec
       ;; if we have a map, just try to pass it through as a vega form
       (if (map? spec) [(or mode :vega-lite) spec] spec)])
    (catch Exception e
      (errorf "error sending plot to server: %s" (ex-data e)))))


(defn ^:no-doc v!
  "Deprecated version of `view!`, which takes a single vega or vega-lite clojure map `spec`, as well as added `:data`,
  `:width` and `:height` options, to be merged into spec priori to `view!`ing."
  [spec & {:as opts
           :keys [data width height host port mode]
           :or {port (:port @server/web-server_ server/default-port)
                host "localhost"
                mode :vega-lite}}]
  ;; Update spec opts, then send view
  (let [spec (merge-opts spec opts)]
    (view! spec :host host :port port :mode mode)))



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

(defn ^:no-doc publish-plot!
  "Deprecated form of `publish!`"
  [plot & opts]
  (warnf "WARNING!!! DEPRECATED!!! Please call `publish!` instead.")
  (let [spec (merge-opts plot opts)]
    (publish! spec opts)))

(defn- ^:no-doc live-embed
  "Embed a specific visualization; Currently private, may be public in future, and name may change."
  ([[mode spec]]
   (let [id (str "viz-" (java.util.UUID/randomUUID))
         code (format "vegaEmbed('#%s', %s, %s);" id (json/generate-string spec) (json/generate-string {:mode mode}))]
     [:div
       [:div {:id id}]
       [:script {:type "text/javascript"} code]])))

(defn ^:no-doc embed
  "Take hiccup or vega/lite spec and embed the vega/lite portions using vegaEmbed, as hiccup :div and :script blocks.
  When rendered, should present as live html page; Currently semi-private, may be made fully public in future."
  ([spec {:as opts :keys [embed-fn mode] :or {embed-fn live-embed mode :vega-lite}}]
   ;; prewalk spec, rendering special hiccup tags like :vega and :vega-lite, and potentially other composites,
   ;; rendering using the components above. Leave regular hiccup unchanged).
   ;; TODO finish writing; already hooked in below so will break now
   (if (map? spec)
     (embed-fn [mode spec])
     (clojure.walk/prewalk
       (fn [x] (if (and (coll? x) (#{:vega :vega-lite} (first x)))
                 (embed-fn x)
                 x))
       spec)))
  ([spec]
   (embed spec {})))


(defn html
  ([spec opts]
   (if (map? spec)
     (html [:vega-lite spec])
     (hiccup/html 
       [:html
        [:head
         [:meta {:charset "UTF-8"}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
         [:link {:rel "shortcut icon" :href "http://ozviz.io/oz.svg" :type "image/x-icon"}]
         [:link {:rel "stylesheet" :href "http://ozviz.io/css/style.css" :type "text/css"}]
         [:link {:rel "stylesheet" :href "http://ozviz.io/fonts/lmroman12-regular.woff"}]
         [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans"}] 
         [:script {:type "text/javascript" :src "https://cdn.jsdelivr.net/npm/vega@5.3.2"}]
         [:script {:type "text/javascript" :src "https://cdn.jsdelivr.net/npm/vega-lite@3.0.2"}]
         [:script {:type "text/javascript" :src "https://cdn.jsdelivr.net/npm/vega-embed@4.0.0"}]]
        [:body
         (embed spec opts)
         [:div#vis-tooltip {:class "vg-tooltip"}]]])))
  ([spec]
   (html spec {})))
   

(defn export!
  "In alpha; Export spec to an html file. May eventually have other options, including svg, jpg & pdf available"
  [spec filepath & {:as opts :keys []}]
  (spit filepath (html spec opts)))


(defn- process-md-block
  [block]
  (if (vector? block)
    (let [[block-type & contents :as block] block]
      (if (= :pre block-type)
        (let [[_ {:keys [class] :or {class ""}} src] (->> contents (remove map?) first)
              classes (->> (string/split class #" ") (map keyword) set)]
          (if-not (empty? (set/intersection classes #{:vega :vega-lite :hiccup :edn-vega :edn-vega-lite :edn-hiccup :json-vega-lite :json-vega :json-hiccup :yaml-vega :yaml-vega-lite}))
            (let [viz-type (cond
                             (set/intersection classes #{:vega :edn-vega :json-vega}) :vega
                             (set/intersection classes #{:vega-lite :edn-vega-lite :json-vega-lite}) :vega-lite
                             (set/intersection classes #{:hiccup :edn-hiccup :json-hiccup}) :hiccup)
                  src-type (cond
                             (set/intersection classes #{:edn :edn-vega :edn-vega-lite :edn-hiccup}) :edn
                             (set/intersection classes #{:json :json-vega :json-vega-lite :json-hiccup}) :json)
                  data (case src-type
                         :edn (edn/read-string src)
                         :json (json/parse-string src keyword)
                         :yaml (yaml/parse-string src))]
              (case viz-type
                :hiccup data
                (:vega :vega-lite) [viz-type data]))
            block))
        block))
    block))
    

(defn- ^:no-doc from-markdown
  "Process markdown string into a hiccup document"
  [md-string]
  (try
    (let [hiccup (-> md-string markdown/md->hiccup (markdown/hiccup-in :html :body) rest)]
      (->> hiccup (map process-md-block) (into [:div])))
    (catch Exception e
      (log/error "Unable to process markdown")
      (.printStackTrace e))))


(defn load
  "Reads file and processes according to file type"
  [filename & {:as opts :keys [format]}]
  (let [contents (slurp filename)]
    (case (or (and format (name format))
              (last (string/split filename #"\.")))
      "md" (from-markdown contents)
      "edn" (edn/read-string contents)
      "json" (json/parse-string contents keyword)
      "yaml" (yaml/parse-string contents))))



;; Refer to the live-reload! function
(clone-var live/live-reload!)
(clone-var live/kill-watcher!)
(clone-var live/kill-watchers!)


;; For the live-view! function below
(defn- view-file!
  [{:keys [host port format]} filename context {:keys [kind file]}]
  ;; ignore delete (some editors technically delete the file on every save!
  (when (#{:modify :create} kind)
    (let [contents (slurp filename)]
      ;; if there are differences, then do the thing
      (when-not (= contents
                   (get-in @live/watchers [filename :last-contents]))
        (log/info "Rerendering file:" filename)
        ;; Evaluate the ns form, and whatever forms thereafter differ from the last time we succesfully ran
        ;; Update last-forms in our state atom
        (view! (load filename :format format) :host host :port port)
        (swap! live/watchers assoc-in [filename :last-contents] contents)))))

(defn live-view!
  "Watch file for changes and apply `load` & `view!` to the contents"
  [filename & {:keys [host port format] :as opts}]
  (live/watch! filename (partial view-file! opts)))


(comment
  (live-view! "examples/test.md" :port 8888)
  (kill-watchers!)
  :end-comment)

(comment
;(try
  (export!
    (load "examples/test.md")
    "examples/test.html")
  (catch Exception e (.printStackTrace e)))


;(do
(comment
  (export!
    [:div
     [:h1 "Greetings, Earthling"]
     [:p "Take us to the King of Kings. Thy kale chips set us free."]
     [:h2 "Look, and behold"]
     [:vega-lite {:data {:values [{:a 2 :b 3} {:a 5 :b 2} {:a 7 :b 4}]}
                  :mark :point
                  :width 400
                  :encoding {:x {:field "a"}
                             :y {:field "b"}}}]]
    ;; Should be using options for mode vega/vega-lite TODO
    "test.html"))


