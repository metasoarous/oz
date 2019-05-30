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
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [yaml.core :as yaml]
            [markdown-to-hiccup.core :as md->hc]
            [markdown-to-hiccup.decode :as md-decode]
            [markdown.core :as md]
            [hickory.core :as hickory]
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
(clone-var server/start-server!)
;; (Deprecated old API)
(defn ^:no-doc start-plot-server!
  [& args]
  (log/warn "DEPRECATED! Please switch from start-plot-server! to start-server!")
  (apply server/start-server! args))

(defonce ^{:private true} cookie-store (clj-http.cookies/cookie-store))
(defonce ^{:private true} anti-forgery-token (atom nil))

(defn- prepare-server-for-view!
  [port host]
  ;; start the webserver if needed
  (when (or (not= (server/get-server-port) port)
            (not (server/web-server-started?)))
    (log/info "Starting up server on port" port)
    (start-server! port))
  (when-not @anti-forgery-token
    (when-let [token (:csrf-token
                      (json/parse-string
                       (:body (client/get (str "http://" host ":" port "/token")
                                          {:cookie-store cookie-store}))
                       keyword))]
      (reset! anti-forgery-token token))))


;; Main view functions

(def mathjax-script
  [:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/MathJax.js?config=TeX-MML-AM_CHTML"}])

(defn- apply-fn-component
  "Takes a spec where the first argument is a function and applies it with the rest of the entries in spec.
  If the result is itself a function (form-2 component, in Reagent speak), then returns result."
  [form]
  (let [result (apply (first form) (rest form))]
    (if (fn? result)
      ;; Then a form 2 component, so immediately call inner return fn
      (apply result (rest form))
      ;; otherwise, assume hiccup and return results
      result)))


(defn- prep-for-live-view
  [spec {:keys [mode]}]
  [:div
    (if (map? spec)
      [(or mode :vega-lite) spec]
      (clojure.walk/prewalk
        (fn [form]
          (cond
            ;; If we have a reagent style fn component, need to call and send just data
            (and (vector? form) (fn? (first form))) 
            (apply-fn-component form)
            ;; Otherwise, just return spec
            :else form))
        spec))
    mathjax-script])


(defn view!
  "View the given spec in a web browser. Specs for which map? is true are treated as single Vega-Lite/Vega specifications.
  All other values are treated as hiccup, and are therefore expected to be a vector or other iterable.
  This hiccup may contain Vega-Lite/Vega visualizations embedded like `[:vega-lite spec]` or `[:vega spec]`.
  You may also specify `:host` and `:port`, for server settings, and a `:mode` option, defaulting to `:vega-lite`, with `:vega` the alternate option.
  (Though I will note that Vega-Embed often catches when you pass a vega spec to a vega-lite component, and does the right thing with it.
  However, this is not guaranteed behavior, so best not to depend on it (wink, nod))"
  [spec & {:keys [host port mode] :as opts}]
  (let [port (or port (server/get-server-port) server/default-port)
        host (or host "localhost")]
    (try
      (prepare-server-for-view! port host)
      (let [hiccup-spec (prep-for-live-view spec opts)]
        ;; if we have a map, just try to pass it through as a vega form
        (server/send-all! [::view-spec hiccup-spec]))
      (catch Exception e
        (log/error "error sending plot to server:" e)
        (log/error "Try using a different port?")
        (.printStackTrace e)))))


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
        auth-file (or (:auth-file args) (live/join-paths (System/getProperty "user.home") ".oz/github-creds.edn"))]
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
       ;; TODO In the future this should be a precompiled version of whatever the viz renders as (svg), at least
       ;; optionally
       [:div {:id id}]
       [:script {:type "text/javascript"} code]])))



(defn ^:no-doc map->style-string
  [m]
  (->> m
       (map #(str (name (first %)) ": " (second %)))
       (string/join "; ")))


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
       (fn [form]
         (cond
           ;; For vega or vega lite apply the embed-fn (TODO add :markdown elements to hiccup documents)
           (and (vector? form) (#{:vega :vega-lite :leaflet-vega :leaflet-vega-lite :markdown} (first form)))
           (embed-fn form)
           ;; Make sure that any style attrs are properly case (newer hiccup should do this, but for now)
           (and (vector? form) (keyword? (first form)) (map? (second form)) (-> form second :style map?))
           (into [(first form)
                  (update (second form) :style map->style-string)]
                 (drop 2 form))
           ;; If we see a function, call it with the args in form
           (and (vector? form) (fn? (first form))) 
           (apply-fn-component form)
           ;; Else, assume hiccup and leave form alone
           :else form))
       spec)))
  ([spec]
   (embed spec {})))


(defn html
  ([spec opts]
   (let [metadata (or (meta spec) {})]
     (if (map? spec)
       (html [:vega-lite spec])
       (hiccup/html
         [:html
          [:head
           [:meta {:charset "UTF-8"}]
           [:title (or (:title metadata) "Oz document")]
           [:meta {:name "description" :content (or (:description metadata) "Oz document")}]
           (when-let [author (:author metadata)]
             [:meta {:name "author" :content author}])
           (when-let [keywords (:keywords metadata)]
             [:meta {:name "keywords" :content (string/join "," (into (set keywords) (:tags metadata)))}])
           [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
           [:link {:rel "shortcut icon" :href "http://ozviz.io/oz.svg" :type "image/x-icon"}]
           [:link {:rel "stylesheet" :href "http://ozviz.io/css/style.css" :type "text/css"}]
           [:link {:rel "stylesheet" :href "http://ozviz.io/fonts/lmroman12-regular.woff"}]
           [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans"}] 
           [:script {:type "text/javascript" :src "https://cdn.jsdelivr.net/npm/vega@5.3.2"}]
           [:script {:type "text/javascript" :src "https://cdn.jsdelivr.net/npm/vega-lite@3.0.2"}]
           [:script {:type "text/javascript" :src "https://cdn.jsdelivr.net/npm/vega-embed@4.0.0"}]
           ;[:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/latest.js?config=TeX-MML-AM_CHTML"}]
           [:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/MathJax.js?config=TeX-MML-AM_CHTML"}]]
          [:body
           (vec (embed spec opts))
           [:div#vis-tooltip {:class "vg-tooltip"}]
           [:script {:src "js/compiled/oz.js" :type "text/javascript"}]]]))))
  ([spec]
   (html spec {})))

(comment
  (spit "resources/oz/public/index.html"
        (html [:div#app [:h2 "oz"] [:p "pay no attention"]]))
  :end-comment)
   

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
                             (seq (set/intersection classes #{:vega :edn-vega :json-vega})) :vega
                             (seq (set/intersection classes #{:vega-lite :edn-vega-lite :json-vega-lite})) :vega-lite
                             (seq (set/intersection classes #{:hiccup :edn-hiccup :json-hiccup})) :hiccup)
                  src-type (cond
                             (seq (set/intersection classes #{:edn :edn-vega :edn-vega-lite :edn-hiccup})) :edn
                             (seq (set/intersection classes #{:json :json-vega :json-vega-lite :json-hiccup})) :json)
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
    (let [{:keys [metadata html]} (md/md-to-html-string-with-meta md-string)
          hiccup (-> html hickory/parse hickory/as-hiccup first md->hc/component md-decode/decode)]
          ;hiccup (-> html hickory/parse hickory/as-hiccup first md->hc/component)]
          ;; TODO deal with encoding of html escape characters (see markdown->hc for this)!
      (with-meta
        (->> hiccup (map process-md-block) vec)
        metadata))
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


(defn drop-extension
  [relative-path]
  (string/replace relative-path #"\.\w*$" ""))

(defn html-extension
  [relative-path]
  (string/replace relative-path #"\.\w*$" ".html"))

(defn- compute-out-path
  [{:as spec :keys [from to out-path-fn]} path]
  (let [out-path-fn (or out-path-fn drop-extension)
        single-file? (= path from)
        to-dir? (or (.isDirectory (io/file to))
                    (= (last (.getPath (io/file path))) (java.io.File/separatorChar)))
        relative-from-path (if single-file? path (live/relative-path path from))]
    (if (and single-file? (not to-dir?))
      ;; then we're just translating a single file with an explicit to path
      to
      ;; then we need to assume that we're exporting to a path which has a directory created for it
      (live/join-paths (or to ".")
                       (out-path-fn relative-from-path)))))


(defn- ensure-out-dir
  [out-path drop-last?]
  (let [split-path (string/split out-path (re-pattern (java.io.File/separator)))
        split-path (if drop-last? (drop-last split-path) split-path)
        intermediate-paths (map-indexed
                             (fn [i _]
                               (string/join (java.io.File/separator) (take (inc i) split-path)))
                             split-path)]
    (doseq [path intermediate-paths]
      (let [file (io/file path)]
        (when-not (.isDirectory file)
          (.mkdir file))))))


(defn extension
  [filename]
  (last (string/split filename #"\.")))

(def supported-filetypes
  #{"md" "mds" "clj" "cljc" "cljs" "yaml" "json" "edn"})

(def asset-filetypes
  #{"jpg" "png" "svg" "css"})


;; For keeping track of builds

(defonce ^:private last-built-file (atom nil))
@last-built-file


(def default-ignore-patterns
  [#".*~$"
   #"^\d*$"
   #"^\..*\.sw\wx?$"])

(defn- ignore?
  [f]
  (some
    #(-> f io/file .getPath (string/split #"\/") last (->> (re-find %)))
    default-ignore-patterns))


(defn- build-and-view-file!
  [{:as config :keys [view? host port force-update]}
   {:as spec :keys [format from to out-path-fn template-fn html-template-fn as-assets?]}
   filename context {:keys [kind file]}]
  (when (and from (.isDirectory (io/file from)))
    (ensure-out-dir to false))
  (if-let [ext (and file (extension (.getPath file)))]
    (cond
      ;; Handle asset case; just copy things over directly
      (or as-assets? (asset-filetypes ext))
      (when (and (not (.isDirectory file)) (#{:modify :create} kind) (not (ignore? file)))
        (let [out-path (compute-out-path (assoc spec :out-path-fn identity)
                                         (.getPath file))]
          (ensure-out-dir out-path true)
          (log/info "updating asset:" file)
          (io/copy file (io/file out-path))))
      ;; Handle the source file case
      (supported-filetypes ext)
      ;; ignore delete (some editors technically delete the file on every save!); also ignore dirs
      (when (and (#{:modify :create} kind) file (not (.isDirectory file)))
        (reset! last-built-file [(live/canonical-path file) spec])
        (let [filename (.getPath file)
              ext (extension filename)
              contents (slurp filename)]
          ;; if there are differences, then do the thing
          (when-not (= contents
                       (get-in @live/watchers [filename :last-contents]))
            (log/info "Rerendering file:" filename)
            (let [evaluation
                  (cond
                    ;; 
                    (#{"clj" "cljc"} ext)
                    (live/reload-file! filename context {:kind kind :file file})
                    ;; how do we handle cljs?
                    (#{"cljs"} ext)
                    [:div "CLJS Coming soon!"]
                    ;; loading of static files, like md or hiccup
                    :else
                    (load filename :format format))
                  ;_ (log/info "step 1:" evaluation)
                  evaluation (with-meta (if template-fn (template-fn evaluation) evaluation) (meta evaluation))
                  ;_ (log/info "step 2:" evaluation)
                  out-path (compute-out-path spec filename)]
              (ensure-out-dir out-path true)
              (when view?
                (log/info "Updating live view")
                (view! evaluation :host host :port port))
              (export! evaluation out-path)
              (swap! live/watchers update filename (partial merge {:last-contents contents :last-eval evaluation})))))))))


(def ^:private default-config
  {:live? true
   :view? true
   :lazy? true
   :port 5760
   :host "localhost"})

(def ^:private default-spec
  {:out-path-fn html-extension})



(defn kill-builds!
  "Not to be confused with Kill Bill..."
  []
  ;; for now this should suffice
  (live/kill-watchers!))


(defn- infer-root-dir
  [specs]
  ;; not correct; mocking
  (->> specs
       (map (comp live/canonical-path :to))
       (reduce live/greatest-common-path)))

(defn build!
  "Builds a static web site based on the content specified in specs. Each spec should be a mapping of paths, with additional
  details about how to build data from one path to the other. Available spec keys are:
    * `:from` - (required) Path from which to build
    * `:to` - (required) Compiled files go here
    * `:template-fn` - Function which takes Oz hiccup content and returns some new hiccup, presumably placing the content in question in some 
    * `:out-path-fn` - Function used for naming compilation output
    * `:as-assets?` - Pass through as a static assets (for images, css, json or edn data, etc)
      - Note: by default, images, css, etc will pass through anyway

  Additional options pertinent to the entire build process may be passed in:
    * `:live?` - Watch the file files 
    * `:lazy?` - If true, don't build anything until it changes; this is best for interactive/incremental updates and focused work.
                 Set to false if you want to rebuild from scratch. (default true)
    * `:view?` - Build with live view of most recently changed file (default true)
    * `:root-dir` - Static assets will be served relative to this directory (defaults to greatest-common-path between all paths)
  "
  ;; lazy? - (This is one that it would be nice to merge in at the spec level)
  ;; future: middleware?
  ([specs & {:as config}]
   (kill-builds!)
   (if (map? specs)
     (mapply build! [specs] config)
     (let [{:as full-config :keys [lazy? live? view?]}
           (merge default-config config)]
       (reset! server/current-root-dir (or (:root-dir config) (infer-root-dir specs)))
       (doseq [spec specs]
         (let [full-spec (merge default-spec spec)]
           ;; On first build, build out all of the results unless lazy? has been passed or we haven't built it
           ;; yet
           (doseq [src-file (file-seq (io/file (:from full-spec)))]
             ;; we don't want to display the file on these initial builds, only for most recent build
             (let [config' (assoc full-config :view? false)
                   dest-file (compute-out-path full-spec src-file)]
               (when (and (not lazy?) (not (.exists (io/file dest-file))))
                 (build-and-view-file! config' full-spec (:from full-spec) nil {:kind :create :file src-file}))))
           ;; Start watching files for changes
           (when live?
             (live/watch! (:from full-spec) (partial build-and-view-file! full-config full-spec)))))
       ;; If we're running this the second time, we want to immediately rebuild the most recently compiled
       ;; file, so that any new templates or whatever being passed in can be re-evaluated for it.
       (when-let [[file spec] @last-built-file]
         (let [new-spec (first (filter #(= (:from %) (:from spec)) specs))]
           (log/info "Recompiling last viewed file:" file)
           (build-and-view-file! full-config new-spec (:from spec) {} {:kind :create :file (io/file file)})))))))

;(reset! last-built-file nil)
;@last-built-file


;; for purpose of examples below

(defn- site-template
  [spec]
  [:div {:style {:max-width 900 :margin-left "auto" :margin-right "auto"}}
   spec])

(defn- blog-template
  [spec]
  (site-template
    (let [{:as spec-meta :keys [title published-at tags]} (meta spec)]
      [:div
       [:h1 {:style {:line-height 1.35}} title]
       [:p "Published on: " published-at]
       [:p "Tags: " (string/join ", " tags)]
       [:br]
       spec])))


;; Some examples for you

(comment
  ;; View a simple plot
  (view!
    {:data {:values [{:a 1 :b 2} {:a 3 :b 5} {:a 4 :b 2}]}
     :mark :point
     :encoding {:x {:field :a}
                :y {:field :b}}}
    :port 10666)

  ;; View a more complex document
  (export!
    [:div
     [:h1 "Greetings, Earthling"]
     [:p "Take us to your King of Kings. We demand tribute!."]
     [:h2 "Look, and behold"]
     [:vega-lite {:data {:values [{:a 2 :b 3} {:a 5 :b 2} {:a 7 :b 4}]}
                  :mark :point
                  :width 400
                  :encoding {:x {:field "a"}
                             :y {:field "b"}}}]]
    ;; Should be using options for mode vega/vega-lite TODO
    "test.html")

  ;; Run live view on a file, and see compiled changes real time
  (live-view! "examples/test.md" :port 8888)
  ;; Can kill file watchers manually if you want
  (kill-watchers!)


  ;; Run static site generation features
  (build!
    [{:from "examples/static-site/src/site/"
      :to "examples/static-site/build/"
      :template-fn site-template}
     ;; If you have static assets, like datasets or imagines which need to be simply copied over
     {:from "examples/static-site/src/assets/"
      :to "examples/static-site/build/"
      :as-assets? true}]
    :lazy? false
    :port 2388)
    ;:root-dir "examples/static-site/build")

  :end-comment)


