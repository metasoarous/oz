(ns oz.core
  (:refer-clojure :exclude [load compile])
  (:require [oz.server :as server]
            [oz.live :as live]
            [clj-http.client :as client]
            [aleph.http :as aleph]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [yaml.core :as yaml]
            [markdown-to-hiccup.core :as md->hc]
            [markdown-to-hiccup.decode :as md-decode]
            [markdown.core :as md]
            [hickory.core :as hickory]
            [hiccup.core :as hiccup]
            [taoensso.timbre :as log]
            [tentacles.gists :as gists]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.core.async :as a]
            [respeced.test :as rt]
            [applied-science.darkstar :as darkstar])
  (:import java.io.File
           java.util.UUID))

(defn- apply-opts
  "utility function for applying kw-args"
  [f & args]
  (apply f (concat (butlast args) (flatten (into [] (last args))))))

  

(defn sample
  ([spec]
   (gen/sample (s/gen spec)))
  ([spec n]
   (gen/sample (s/gen spec) n)))

(defn spy
  [x]
  (println x)
  x)


;; Speccing out domain

(s/def ::vega-cli-mode #{:vega :vega-lite})

;; Still feels to me like we're missing something big semantically here as far as the difference between a
;; mode and a format

;; Should review ::mode and see how much we actually use it vs from-format and to-format
(s/def ::mode
  (s/with-gen keyword?
    #(s/gen #{:vega :vega-lite :hiccup})))

(s/def ::format
  (s/with-gen keyword?
    #(s/gen #{:edn :json :yaml :html :pdf :png :svg})))

;; Not sure if I like the way that this is specced
(s/def ::from-format
  (s/with-gen
    (s/or :mode ::mode :format ::format)
    #(s/gen #{:edn :json :yaml :hiccup :vega :vega-lite}))) ;; add html & svg?

(s/def ::to-format
  (s/with-gen
    (s/or :mode ::mode :format ::format)
    #(s/gen #{:edn :json :yaml :hiccup :vega :svg :html})))


(declare load)

(defn example-generator
  [mode]
  (s/gen
    (->>
      (file-seq (io/file (io/resource (str "oz/examples/" (name mode)))))
      (remove #(.isDirectory %))
      (map str)
      (map load)
      set)))

(s/def ::vega-lite
  (s/with-gen map?
    (fn [] (example-generator :vega-lite))))

(s/def ::vega
  (s/with-gen map?
    (fn [] (example-generator :vega))))

(s/def ::vega-like
  (s/or :vega-lite ::vega-lite :vega ::vega))

(deftest ^:no-doc exercise-vega
  (is (sample ::vega))
  (is (s/exercise ::vega-like)))

(s/def ::tag
  (s/with-gen
    (s/or :keyword keyword? :string string?)
    (fn [] (s/gen #{:div :h1 :p :code :foo :bar}))))

(s/def ::hiccup
  (s/with-gen
    (s/and
      vector?
      (s/cat :tag ::tag :body (s/* any?)))
    (fn []
      (gen/fmap
        (fn [form]
          (vec form))
        (s/gen (s/cat :tag ::tag :body (s/* any?)))))))

;; Translate to tests?
(deftest ^:no-doc exercise-hiccup
  (is (s/exercise ::hiccup))
  (is (s/conform ::hiccup [:div {:styles {}} [:h1 "some shit"] [:p "ya know?"]])))


(s/def ::document (s/or :hiccup ::hiccup :vega map?))

(deftest ^:no-doc exercise-document
  (is (s/exercise ::document)))

(s/def ::input-filename string?)
(s/def ::output-filename string?)
(s/def ::return-result? boolean?)
(s/def ::scale pos?)


; Spec for vega-cli

(s/def ::vega-cli-opts
  (s/keys :req-un [(or ::vega ::vega-lite ::input-filename) ::to-format]
          :opt-un [::from-format ::return-result? ::output-filename ::scale]))

(s/def ::vega-compiler
  #{:vega-cli :graal})

(s/def ::base-vega-compile-opts
  (s/keys :opt-un []))

(defn ^:no-doc choose-vega-compiler
  [{:keys [vega-compiler to-format]}]
  (or vega-compiler
      ;; Prefer grall for svg and vega output
      (if (#{:svg :vega} to-format)
        :graal
        :vega-cli)))

(defmulti vega-compile-opts-spec
  choose-vega-compiler)

(defmethod vega-compile-opts-spec
  :graal
  [_] map?)

(defmethod vega-compile-opts-spec
  :vega-cli
  [_] ::vega-cli-opts)

(s/def ::vega-compile-opts
  (s/multi-spec vega-compile-opts-spec (fn [genval _] genval)))


(deftest ^:no-doc exercise-vega-cli-opts
  (is (s/exercise ::vega-cli-opts))
  (is (s/exercise ::vega-compile-opts)))


(s/def ::title string?)
(s/def ::description string?)
(s/def ::author string?)
(s/def ::tags (s/coll-of string?))
(s/def ::keywords (s/coll-of string?))
(s/def ::shortcut-icon-url string?)
         
(s/def ::omit-shortcut-icon? boolean?)
(s/def ::omit-styles? boolean?)
(s/def ::omit-charset? boolean?)
(s/def ::omit-vega-libs? boolean?)
;; Leaving these options out for now
;(s/def ::omit-mathjax? boolean?)
;(s/def ::omit-js-libs? boolean?)

(s/def ::head-extras ::hiccup)

(def vega-version "5.20.0")
(def vega-lite-version "5.1.0")
(def vega-embed-version "6.12.2")


(s/fdef vega-cli-installed?
        :args (s/cat :mode ::vega-cli-mode)
        :ret boolean?
        :fn (fn [{:keys [args ret]}]
              ret))

(def ^:no-doc installed-clis
  (atom {}))

(defn ^:no-doc -check-vega-cli-installed? [mode]
  (= 0 (:exit (shell/sh "which" (case mode :vega-lite "vl2svg" :vega "vg2svg")))))

(defn- vega-cli-installed? [mode]
  ;; First checks the installed-clis cache, then epirically checks via cli call
  (or (get @installed-clis mode)
      (let [status (-check-vega-cli-installed? mode)]
        ;; updating cache means once we install, we don't have to run cli check anymore
        (swap! installed-clis mode status)
        status)))

(deftest ^:no-doc test-vega-cli-installed?
  (is (= true (vega-cli-installed? :vega)))
  (is (= true (vega-cli-installed? :vega-lite))))

;; Utils

(defn- doc-type [doc]
  (if (sequential? doc) :ozviz :vega))

(def ^{:private true} vega-doc-opts
  #{:data :width :height :datasets})

(defn- merge-opts
  "Merge relevant api opts into vega data structure, removing entries with nil values"
  [doc opts]
  (->> opts
       (filter (comp vega-doc-opts first))
       (remove (comp nil? second))
       (into doc)))

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
(declare start-server!) ;; for linting
(clone-var server/start-server!)

;; (Deprecated old API)
(defn ^:no-doc start-plot-server!
  [& args]
  (log/warn "DEPRECATED! Please switch from start-plot-server! to start-server!")
  (apply server/start-server! args))


(defn- prepare-server-for-view!
  [port host]
  ;; start the webserver if needed
  (when (or (not= (server/get-server-port) port)
            (not (server/web-server-started?)))
    (log/info "Starting up server on port" port)
    (start-server! port)))


;; Main view functions

(def ^:private mathjax-script
  [:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/MathJax.js?config=TeX-MML-AM_CHTML"}])

(defn- apply-fn-component
  "Takes a hiccup-form where the first argument is a function and applies it with the rest of the entries in form
  If the result is itself a function (form-2 component, in Reagent speak), then returns result."
  [form]
  (let [result (apply (first form) (rest form))]
    (if (fn? result)
      ;; Then a form 2 component, so immediately call inner return fn
      (apply result (rest form))
      ;; otherwise, assume hiccup and return results
      result)))


(s/def ::tag-compiler
  (s/with-gen
    (s/fspec :args (s/cat :form ::hiccup) :ret any?)
    (fn []
      (s/gen
        #{(fn [[_ & rest]] [:blah [:zippy "Never gonna give you up"] rest])
          (fn [_] [:elided "nothing to see here"])}))))
          

(s/def ::tag-compilers
  (s/map-of ::tag ::tag-compiler))

(deftest ^:no-doc exercise-tag-compiler
  (is (s/exercise ::tag-compiler)))

;; TODO QUESTION
;; This is probably not quite right; We should probably err on the side of requiring these args for now

(s/def ::base-compile-opts
  (s/keys :opt-un [::to-format ::from-format ::mode ::tag-compilers]))

;; We use this multimethod for the underlying implementation

(defn- extension
  [filename]
  (last (string/split filename #"\.")))

;; QUESTION Does this need to be pluggable?
(def ^:no-doc extension-formats
  {:md :markdown})

(defn- compiler-key
  ([doc {:keys [from-format mode to-format]}]
   (let [from-format (or from-format mode (cond (vector? doc) :hiccup (map? doc) :vega-lite))]
     [(get extension-formats from-format from-format)
      (or to-format :hiccup)])))

(defmulti ^:no-doc compile-args-spec
  (partial apply compiler-key))

;; Warning! Changing the defmulti above doesn't take on reload! Have to restart repl :-/
(s/def ::compile-args
  (s/and (s/multi-spec compile-args-spec (fn retag [genval _] genval))
         #(s/valid? ::registered-compiler-key (apply compiler-key %))))


(defmulti to-spec :to)
(defmethod to-spec :default
  [{:keys [to-format]}]
  (keyword "oz.core" (name (or to-format :hiccup))))


(s/fdef compile*
  :args ::compile-args
  :fn (fn [{:keys [args ret]}]
        (let [[_ opts] args]
          (s/valid? (to-spec opts) ret))))


;; We will eventually need to call out to compile in some of the definitions
(declare compile)

(defmulti ^:no-doc compile*
  "General purpose compilation function which turns things from-format one data structure into another"
  {:arglists '([doc {:keys [from-format to-format]}])}
  compiler-key)

;; QUESTION How do we merge the tag-compilers options for html compilation embedding?
;; * merge in options as we go when we are compiling to html?
;;   -> :to html should be the one in charge of embed type compilers
;;   -> :to html should be the one in charge of wrap-html when needed


;(rt/successful? (rt/check `compile {} {:num-tests 10}))


;; Questions:
;; * Do I want to pass in through stdin or through a file?
;; * Do I want to write out through a file or through stdout?
;; * Do png, svg & pdf have file output options or just export!?

;; * Why am I getting back "" for png?


;(json/encode {:this {:that/the "hell"}})

(defn- tmp-filename
  [ext]
  (str (java.io.File/createTempFile (str (java.util.UUID/randomUUID)) (str "." (name ext)))))


;; QUESTION What do we call the abstract `:vega-doc` here?

(defn file->bytes [file]
  (with-open [xin (io/input-stream file)
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(defn ^:no-doc bytes->file
  [file bytes]
  (with-open [out (io/output-stream file)]
    (.write out bytes)))

(defn vega-cli
  "Takes either doc or the contents of input-filename, and uses the vega/vega-lite cli tools to translate to the specified format.
  If both doc and input-filename are present, writes doc to input-filename for running cli tool (otherwise, a tmp file is used).
  This var is semi-public; It's under consideration for fully public inclusion, but consider it alpha for now."
  ([{:keys [vega-doc from-format to-format mode input-filename output-filename return-result? scale] ;; TODO may add seed eventually
     :or {to-format :svg mode :vega-lite return-result? true scale 1}}]
   {:pre [(#{:vega-lite :vega} (or from-format mode))
          (#{:png :pdf :svg :vega} to-format)
          (or vega-doc input-filename)]}
   (let [mode (or from-format mode)]
     (if (vega-cli-installed? mode)
       (let [short-mode (case (keyword mode) :vega-lite "vl" :vega "vg")
             ext (name (if (= to-format :vega) :vg to-format))
             input-filename (or input-filename (tmp-filename (str short-mode ".json")))
             output-filename (or output-filename (tmp-filename ext))
             command (str short-mode 2 ext)
             ;; Write out the vega-doc file, and run the vega(-lite) cli command
             _ (when vega-doc
                 (spit input-filename (json/encode vega-doc)))
             {:keys [out exit err]} (shell/sh command (str "-s" scale) input-filename output-filename)]
         (log/info "input:" input-filename)
         (log/info "output:" output-filename)
         (if (= exit 0)
           (when return-result?
             (cond
               (= :vega to-format)      (json/parse-stream (io/reader output-filename))
               (#{:png :pdf} to-format) (file->bytes output-filename)
               (= :svg to-format)       (-> output-filename slurp hickory/parse hickory/as-hiccup first md->hc/component last)))
          ;hiccup (-> html hickory/parse hickory/as-hiccup first md->hc/component)]
           (do
             (log/error "Problem creating output")
             (log/error err)
             err)))
       (log/error "Vega CLI not installed! Please run `npm install -g vega vega-lite vega-cli` and try again. (Note: You may have to run with `sudo` depending on your npm setup.)")))))
       ;; Todo; should be throwing


;; Vega-Lite compilations

(defmethod compile-args-spec [:vega-lite :png]
  [_] (s/cat :doc ::vega-lite :opts ::vega-compile-opts))

(defmethod compile* [:vega-lite :png]
  ([doc opts] (vega-cli (merge opts {:vega-doc doc}))))

(defmethod compile-args-spec [:vega-lite :svg]
  [_] (s/cat :doc ::vega-lite :opts ::vega-compile-opts))
(defmethod compile* [:vega-lite :svg]
  ;([doc opts] (vega-cli (merge opts {:vega-doc doc})))
  ([doc _] (darkstar/vega-lite-spec->svg (json/encode doc))))

(defmethod compile-args-spec [:vega-lite :vega]
  [_] (s/cat :doc ::vega-lite :opts ::vega-compile-opts))
(defmethod compile* [:vega-lite :vega]
  ([doc opts] (vega-cli (merge opts {:vega-doc doc}))))


;; Vega compilations

(defmethod compile-args-spec [:vega :png]
  [_] (s/alt :doc ::vega :opts ::vega-compile-opts))
(defmethod compile* [:vega :png]
  ([doc opts] (vega-cli (merge opts {:vega-doc doc}))))

(defmethod compile-args-spec [:vega :svg]
  [_] (s/cat :doc ::vega :opts ::vega-compile-opts))
(defmethod compile* [:vega :svg]
  ;([doc opts] (vega-cli (merge opts {:vega-doc doc})))
  ([doc _] (darkstar/vega-spec->svg (json/encode doc))))

(defmethod compile-args-spec [:vega :pdf]
  [_] (s/cat :doc ::vega :opts ::vega-compile-opts))
(defmethod compile* [:vega :pdf]
  ([doc opts] (vega-cli (merge opts {:vega-doc doc}))))

;(sample (s/cat :doc ::vega-lite :opts ::vega-compile-opts))


;; For Vega-Lite the cli doesn't let us export to pdf, so we have to define this in terms of compiling from
;; Vega-Lite -> Vega -> PDF

;; TODO QUESTION
;; This is almost right; But the language around which opts it applies to is not clear yet
;(s/def ::nested-vega-opts ::vega-compile-opts)

;; This is the right way to do this if we need it
;(s/def ::vega-pdf-opts
  ;(s/keys :req-un []))
  

(defmethod compile-args-spec [:vega-lite :pdf]
  ;; As written below, it may not be incorrect, but files may be getting overwritten between steps or weird
  ;; things happen?
  [_] (s/cat :doc ::vega-lite :opts ::vega-compile-opts))
  ;; If needed, here's an alternate implementation which lets us have nested params for pdf
  ;; Interesting case that exposes why this can't always be so simple. Because the same args
  ;; might get interpretted in multiple ways; Like usage of the output filename stuff
  ;; Basically, we need to s/merge in with the a nested option;
  ;[_] (s/cat :doc ::vega-lite :opts (s/merge ::vega-compile-opts (s/keys :opt-un [::vega-pdf-opts])))

;(sample (s/cat :doc ::vega-lite :opts ::vega-compile-opts))
(defmethod compile* [:vega-lite :pdf]
  ([doc opts]
   (compile*
     (compile* doc (merge opts {:from-format :vega-lite :to :vega}))
     (merge opts {:from-format :vega :to :pdf}))))


;; Defining tag-compilers

(defn- compiled-form
  "Processes a form according to the given processors map, which maps tag keywords
  to a function for transforming the form."
  [processors [tag & _ :as form]]
  (let [tag (keyword tag)]
    (if-let [processor (get processors tag)]
      (processor form)
      form)))

(s/fdef compile-tags
        :args (s/cat :doc ::hiccup :compilers ::tag-compilers)
        :ret ::hiccup)

(defn- compile-tags
  [doc
   compilers]
  (clojure.walk/prewalk
    (fn [form]
      (cond
        ;; If we see a function, call it with the args in form
        (and (vector? form) (fn? (first form))) 
        (compile-tags (apply-fn-component form) compilers)
        ;; apply compilers
        (vector? form)
        (compiled-form compilers form)
        ;; Else, assume hiccup and leave form alone
        :else form))
    doc))

(deftest ^:no-doc test-apply-tag-compilers
  (is (rt/successful? (rt/check `compile-tags {} {:num-tests 5}))))


;; Spec out transformations once ingested into hiccup

;; we'll see how this lives up as a general concept

(s/def ::hiccup-opts
  (s/keys :req-un [::tag-compilers]))

;; TODO Sort out:
;; It sorta bugs me that the application of the tag compilers has to happen in two places (both the hiccup ->
;; hiccup, and the :default, both below).
;; This suggests we may not have the right model here yet
;; Maybe its fine that these things are 

(defmethod compile-args-spec [:hiccup :hiccup]
  [_] (s/cat :doc ::hiccup :opts ::hiccup-opts))
;(sample(s/cat :doc ::hiccup :opts ::hiccup-opts))

(defmethod compile* [:hiccup :hiccup]
  ([doc {:as opts :keys [tag-compilers]}]
   (compile-tags doc tag-compilers)))

(defmethod compile-args-spec :default
  [_] (s/cat :doc ::document :opts ::base-compile-opts))
;(sample (s/cat :doc ::document :opts ::base-compile-opts))


(s/def ::compiler-key
  (s/cat :from-format ::from-format :to-format ::to-format))


;; All of this available/registered business is not particularly performant-savy
;; Is it worth making this a materialized view?
;; This may require an abstracted format registration process

(defn- installed-compiler-keys []
  (->> (methods compile*)
       keys
       (filter (partial s/valid? ::compiler-key))))

;; QUESTION; Keep public?
(defn registered-from-formats []
  (->> (installed-compiler-keys) (map first) set))

(defn registered-to-formats []
  (->> (installed-compiler-keys) (map second) set))


(defn- registered-from-format? [fmt]
  (some #{fmt} (registered-from-formats)))

(defn- registered-to-format? [fmt]
  (some #{fmt} (registered-to-formats)))



(defn registered-compiler-key? [[k1 k2]]
  (let [keys (installed-compiler-keys)]
    (or (some #{[k1 k2]} keys)
        (let [to-hiccup (filter (comp #{:hiccup} second) keys)
              from-hiccup (filter (comp #{:hiccup} first) keys)]
            (and (some (comp #{k1} first) to-hiccup)
                 (some (comp #{k1} first) from-hiccup))))))

(defn registered-compiler-keys []
  (let [keys (installed-compiler-keys)
        from-hiccup (map second (filter (comp #{:hiccup} first) keys))
        to-hiccup (map first (filter (comp #{:hiccup} second) keys))
        implied-keys (for [k1 to-hiccup
                           k2 from-hiccup]
                       [k1 k2])]
    (set (concat keys implied-keys))))

(s/def ::registered-compiler-key
  (s/with-gen
    registered-compiler-key?
    #(s/gen (registered-compiler-keys))))

(s/def ::registered-from-format
  (s/with-gen registered-from-format?
    #(s/gen (set (registered-from-formats)))))

(s/def ::registered-to-format
  (s/with-gen registered-to-format?
    #(s/gen (set (registered-to-formats)))))

;; Compiling hiccup with vega etc in it


(s/def ::live-embed? boolean?)
(s/def ::static-embed
  (s/or :bool boolean?
        :format #{:svg :png}))

(s/def ::vega-embed-opts
  (s/keys
    :opt-un [::static-embed
             ::live-embed?
             ::scale]))

(defn- base64-encode [bytes]
  (.encodeToString (java.util.Base64/getEncoder) bytes))
   
(defn embed-png
  [bytes]
  [:img
   {:alt "compiled vega png"
    :src (str "data:image/png;base64," (base64-encode bytes))}])


(def ^:no-doc default-embed-opts
  {:static-embed :png
   :live-embed? true
   :scale 1})

(defn embed-vega-form
  "Embed a single Vega-Lite/Vega visualization as hiccup representing a live/interactive embedding as hiccup;
  Currently private, may be public in future, and name may change."
  ([compile-opts [mode doc & [embed-opts]]]
   (let [{:as opts :keys [static-embed live-embed?]} (merge default-embed-opts (:vega-embed-opts compile-opts) embed-opts)
         ;; expose id in opts?
         id (str "viz-" (java.util.UUID/randomUUID))
         code (format "vegaEmbed('#%s', %s, %s);" id (json/generate-string doc) (json/generate-string {:mode mode}))]
     [:div
       ;; TODO In the future this should be a precompiled version of whatever the viz renders as (svg), at least
       ;; optionally
       ;; Also SECURITY!!! We should have to allowlist via metadata or something things that we don't want to
       ;; sanitize
       [:div {:id id}
        (when static-embed
          (let [embed-as (s/conform ::static-embed static-embed)]
            (if (= embed-as ::s/invalid)
              "Unable to compile viz"
              (try
                (let [[opt-type opt-val] embed-as]
                  (cond
                    ;; default to png, since this will generally be more performant (TODO: test?)
                    (or (and (= opt-type :bool) opt-val)
                        (= opt-val :png))
                    (embed-png (compile doc (merge opts {:from-format mode :to-format :png})))
                    ;; Use svg as hiccup if requested
                    (= opt-val :svg)
                    (compile doc (merge opts {:from-format mode :to-format :svg}))))
                (catch Throwable t
                  (log/error "Unable to execute static embed")
                  (log/error t)
                  nil)))))]
       (when live-embed?
         [:script {:type "text/javascript"} code])])))


(defn ^:no-doc map->style-string
  [m]
  (->> m
       (map #(str (name (first %)) ": " (second %)))
       (string/join "; ")))

;(s/def ::embed-opts
  ;(s/keys :req-un [::live-embed? ::static-embed?]))

(defn- pprint-hiccup
  [data]
  [:pre (with-out-str (pp/pprint data))])

(defn- print-hiccup
  [data]
  [:code (pr-str data)])


(defn ^:no-doc embed-for-html
  "This is a semi-private function that takes a hiccup or vega document and uses [[compile-tags]] to return hiccup that:
  * embeds any vega documents, by default as both a compiled png and as a live view
  * compiled `:markdown` or `:md` blocks to hiccup
  * `:pprint` blocks as pretty printed strings in pre blocks
  * `:print` blocks similarly"
  ([doc compile-opts]
   (if (map? doc)
     (embed-for-html [:vega-lite doc] (compile-opts))
     (compile-tags doc
                   {:vega (partial embed-vega-form compile-opts)
                    :vega-lite (partial embed-vega-form compile-opts)
                    :markdown #(compile (second %) (merge compile-opts {:from-format :md :to-format :hiccup}))
                    :md       #(compile (second %) (merge compile-opts {:from-format :md :to-format :hiccup}))
                    :pprint   (comp pprint-hiccup second)
                    :print    (comp print-hiccup second)})))
                    ;; TODO Add these; Will take front end resolvers as well
                    ;:leaflet-vega (partial embed-vega-form compile-opts)
                    ;:leaflet-vega-lite (partial embed-vega-form compile-opts)}))
  ([doc]
   (embed-for-html doc {})))

(deftest ^:no-doc functions-as-components
  (testing "should process nested tags"
    (is (= [:div {} [:p {} "yo " [:strong {} "dawg"]]]
           (embed-for-html [(fn [] [:md "yo **dawg**"])])))))

(defn- shortcut-icon [url]
  [:link {:rel "shortcut icon"
          :href url
          :type "image/x-icon"}])

;; which should take precedance in general? meta or html-opts?
;; What about if it's something that could theoretically get merged? like keywords/tags?


(s/def ::html-head-opts
  (s/keys :opt-un [::title ::description ::author ::keywords ::shortcut-icon-url ::omit-shortcut-icon? ::omit-styles? ::omit-charset? ::omit-vega-libs? ::head-extras]))

;; Might be worth exposing this in the future, but uncertain whether this is a good idea for now
(defn- html-head
  "Construct a header as hiccup, given the html opts and doc metadata"
  [doc {:as opts :keys [title description author keywords shortcut-icon-url omit-shortcut-icon? omit-styles? omit-charset? omit-vega-libs? header-extras]}]
  (let [metadata (or (meta doc) {})
        opts (reduce
               (fn [opts' k]
                 (assoc opts' k (or (get opts' k)
                                    (get metadata k))))
               opts
               [:title :description :author :keywords])
        keywords (into (set (:keywords opts))
                       (:tags metadata))]
    (vec
      (concat
        ;; Should we have a separate function for constructing the head?
        [:head
          (when-not omit-charset?
            [:meta {:charset "UTF-8"}])
          [:title (or (:title opts) "Oz document")]
          [:meta {:name "description" :content (or (:description opts) "Oz document")}]
          (when-let [author (:author opts)]
            [:meta {:name "author" :content author}])
          (when keywords
            [:meta {:name "keywords" :content (string/join "," keywords)}])
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          (when-not omit-shortcut-icon?
            (shortcut-icon (or shortcut-icon-url "http://ozviz.io/oz.svg")))]
        ;; QUESTION Possible to embed these directly?
        (when-not omit-styles?
          [
           ;[:link {:rel "stylesheet" :href "http://ozviz.io/css/style.css" :type "text/css"}]
           [:style (slurp (io/resource "oz/public/css/style.css"))]
           [:link {:rel "stylesheet" :href "http://ozviz.io/fonts/lmroman12-regular.woff"}]
           [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans"}]]) 
        ;; TODO Ideally we wouldn't need these, and inclusion of the compiled oz target should be enough; However,
        ;; we're not currently actually included that in html export, so this is necessary for now.
        ;; Everntually though...
        (when-not omit-vega-libs?
          [[:script {:type "text/javascript" :src (str "https://cdn.jsdelivr.net/npm/vega@" vega-version)}]
           [:script {:type "text/javascript" :src (str "https://cdn.jsdelivr.net/npm/vega-lite@" vega-lite-version)}]
           [:script {:type "text/javascript" :src (str "https://cdn.jsdelivr.net/npm/vega-embed@" vega-embed-version)}]])
        ;; Not allowing this option for now;
        ;(when-not omit-js-libs?
        ;[:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/latest.js?config=TeX-MML-AM_CHTML"}]
        [[:script {:type "text/javascript" :src "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/MathJax.js?config=TeX-MML-AM_CHTML"}]]
        header-extras))))


;; Would like to expose this in the future, but not certain about the name/api
;; TODO Add some checks to see if we actually need html/body
(defn- wrap-html
  [doc opts]
  (if-not (= :html (first doc))
    ;; we only wrap with html tag if needed
    [:html
     (html-head doc opts)
     ;; We only wrap with body tag if needed
     (if-not (= :body (first doc))
       [:body doc]
       doc)]
       ;; Don't think we need this anymore
       ;[:div#vis-tooltip {:class "vg-tooltip"}]]]
       ;; TODO This shouldn't be included as such for exported html content (I think it raises a warning)
       ;; Then again, should we just include this instead of the vega libs?
       ;[:script {:src "js/oz.js" :type "text/javascript"}]]]
    doc))


(defn html
  ([doc {:as opts :keys [from-format mode]}]
   (if (map? doc)
     (html [(or from-format mode :vega-lite) doc] opts)
     (-> doc
         (embed-for-html opts)
         (wrap-html opts)
         hiccup/html)))
  ([doc]
   (html doc {})))

(defmethod compile* [:vega-lite :html]
  ([doc opts]
   (html doc (assoc opts :from-format :vega-lite))))

(defmethod compile* [:vega :html]
  ([doc opts]
   (html doc (assoc opts :from-format :vega))))

(s/def ::html-output-opts
  (s/merge ::html-head-opts ::html-embed-opts ::vega-cli-opts))

(defmethod compile-args-spec [:hiccup :html]
  ([_] (s/cat :doc ::hiccup :opts ::html-output-opts)))

(defmethod compile* [:hiccup :html]
  ([doc opts] (html doc opts)))

(comment
  ;; WARNING!!!
  ;; This isn't quite right; We need to be using global references for the stylesheets and fonts, or embedding
  ;; them somehow. But would be good to eventually do something like this as propper build target, or just as
  ;; uncommented code, so that vega versions and such are always up to date.
  (spit "resources/oz/public/index.html"
        (html [:div#app [:h2 "oz"] [:p "pay no attention"]]
              {:omit-vega-libs? true}))
  :end-comment)
   

;; again is this the right naming convention

(defmulti export!*
  {:arglists '([doc filepath {:as opts :keys [to-format]}])}
  (fn [doc _ {:as opts}]
    (second (compiler-key doc opts))))

;(defmulti export!
  ;"In alpha; Export doc to an html file. May eventually have other options, including svg, jpg & pdf available"
  ;[doc filepath & {:as opts}])

(defn filename-format
  [filename]
  (keyword (extension filename)))


(defmethod export!*
  :default
  [doc filepath {:as opts}] 
  ;; Default method is to just call convert and spit to file
  (spit filepath (compile doc opts)))


(defn export!
  "Compile `doc` to `filepath` according to `opts` map. If `:to-format` is not specified, format is
  inferred from the `filepath` extension.

  Default behavior is to call `compile` on the doc, and spit the results to `filepath`. Thus, `opts`
  map will in general be processed as with `compile`.

  As with compile, by default maps `doc` values will be assumed to be `:vega-lite`, unless `:mode`
  or `:from-format` opts are explicitly set to `:vega`.

  ALPHA FEATURE: You may override the export processing for a particular value of `:to-format` using the
  export!* multimethod. However, as with `compile*`, this ability may be superceded by a more robust
  registration function  in the future, so use at your own risk."
  ([doc filepath]
   (export! doc filepath {}))
  ([doc filepath {:as opts :keys [to-format]}]
   ;; Infer the to-format based on filename, unless explicitly set
   ;; TODO Consider what to do if we get a weird filename (no suffix?)
   (let [to-format (or to-format (filename-format filepath))]
     (export!* doc filepath (merge opts {:to-format to-format}))))
  ([doc filepath opt-key opt-val & more-opts]
   (export! doc filepath (merge {opt-key opt-val} more-opts))))

(defmethod export!* :png
  ([doc filepath {:as opts}]
   (let [from-format (first (compiler-key doc opts))]
     (vega-cli (merge opts
                      {:vega-doc doc
                       :to-format :png
                       :output-filename filepath
                       :return-result? false})))))

(comment
  (export!
    {:data {:values [{:a 1 :b 2} {:a 3 :b 4}]}
     :mark :point
     :encoding {:x {:field :a}
                :y {:field :b}}}
    "test.svg")
  (export!
    (read-string (slurp "resources/oz/examples/vega/basic-vega.edn"))
    "vega-test.png"
    :mode :vega)
  (export!
    {:data {:values [{:a 1 :b 2} {:a 3 :b 4}]}
     :mark :point
     :encoding {:x {:field :a}
                :y {:field :b}}}
    "vl-test.png")
  (export!
    [:div [:h1 "Hello, Dave"] [:p "Why are you doing that Dave?"]]
    "dave.html")
  :end-comment)


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
                             (seq (set/intersection classes #{:json :json-vega :json-vega-lite :json-hiccup})) :json
                             (seq (set/intersection classes #{:yaml :yaml-vega :yaml-vega-lite :yaml-hiccup})) :yaml)
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
    


;; I'd like to be able to define my own markdown extensions
;; Maybe via instaparse?
;; Roam data?

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



(s/def ::markdown string?)
(s/def ::edn-string string?)
(s/def ::json-string string?)
(s/def ::yaml-string string?)


(defmethod compile-args-spec [:markdown :hiccup]
  [_] (s/cat :doc ::markdown :opts map?))
(defmethod compile* [:markdown :hiccup]
  ([doc _] (from-markdown doc)))

(defmethod compile-args-spec [:edn :hiccup]
  [_] (s/cat :doc ::edn-string :opts map?))
(defmethod compile* [:edn :hiccup]
  ([doc _] (edn/read-string doc)))

(defmethod compile-args-spec [:json :hiccup]
  [_] (s/cat :doc ::json-string :opts map?))
(defmethod compile* [:json :hiccup]
  ([doc _] (json/parse-string doc keyword)))

(defmethod compile-args-spec [:yaml :hiccup]
  [_] (s/cat :doc ::yaml-string :opts map?))
(defmethod compile* [:yaml :hiccup]
  ([doc _] (yaml/parse-string doc)))


(defn load
  "Reads file and processes according to file type"
  [filename & {:as opts :keys [format from-format]}]
  (let [from-format (or from-format format (keyword (extension filename)))
        doc (slurp filename)]
    (compile doc
             (merge opts
                    {:from-format (or from-format format)}))))

(defmethod compile* :default
  ([doc {:as opts :keys [tag-compilers]}]
   (let [[from-format to-format :as key] (compiler-key doc opts)]
     (println "on:" key)
     (cond-> doc
       (not= :hiccup from-format) (compile* (merge opts {:from-format from-format :to-format :hiccup}))
       tag-compilers              (compile* (merge opts {:from-format :hiccup :to-format :hiccup}))
       (not= :hiccup to-format)   (compile* (-> opts (merge {:from-format :hiccup :to-format to-format}) (dissoc :tag-compilers)))))))

(comment
  (s/conform ::compiler-key [:hiccup :html])

  (def bi (java.awt.image.BufferedImage. 16 16 java.awt.image.BufferedImage/TYPE_INT_ARGB))
  (with-open [in (io/input-stream "testing.png")
              out (io/output-stream "bloop.png")])
  (into [] (io/input-stream "testing.png"))
  :end-comment

  ;; Are we doing this right? We're getting back a string instead of a map...
  (compile
    {:data {:values [{:a 1 :b 2} {:a 3 :b 5} {:a 4 :b 2}]}
     :mark :point
     :encoding {:x {:field :a}
                :y {:field :b}}}
    {:from-format :vega-lite
     :to-format :png})
  (vega-cli
    {:vega-doc
      {:data {:values [{:a 1 :b 2} {:a 3 :b 5} {:a 4 :b 2}]}
       :mark :point
       :encoding {:x {:field :a}
                  :y {:field :b}}}
     :to-format :png
     :output-filename "testing.png"
     :return-result? false})
  (compile
    [:div [:poop "yo dawg"]]
    {:tag-compilers {:poop (fn [_] [:blah "BLOOP"])}
     :to-format :html})
  (rt/successful? (rt/check `compile {} {:num-tests 10}))
  (sample ::compile-args 1)
  :done-comment)


(s/fdef compile
  :args ::compile-args
  :fn (fn [{:keys [args ret]}]
        (let [[_ opts] args]
          (s/valid? (to-spec opts) ret))))

(defn compile
  "General purpose compilation function. Uses `:from-format` and `:to-format` parameters of `opts`
  map to determine how to compile the `doc` argument. If `:from-format` is not specified, `:vega-lite`
  is assumed for maps and `:hiccup` for vectors; `:to-format` option _must_ be specified.

  If you are working with `:hiccup` (even as an intermediary, say between :markdown and :html), the
  `:tag-compilers` map may specify a map of hiccup tags to functions taking and returning hiccup forms.

  Specific compilations may support additional features. For example, compiling to `:html` will support
  all of the options that the `oz/html` function supports.
  
  ALPHA FEATURE: This function can be extended by implementing the `compile*` method on key
  [from-format to-format]. If you define one of these methods to or from `:hiccup`, it will automatically
  be possible to compile from or to any other format for which `:hiccup` already has a compiler
  definition. This functionality may be superceded by a registration function/API in the future, for
  more robust registration of specs, available options documentation, etc."
  {:arglists '([doc & {:keys [from-format to-format tag-compilers]}])}
  ([doc {:as opts :keys [tag-compilers]}]
   ;; Support mode or from-format to `compile`, but require compile* registrations to use `:from-format`
   ;; This is maybe why we _do_ need this function
   (let [[from-format to-format :as key] (compiler-key doc opts)]
     ;(log/debug "compile key is" (with-out-str (pp/pprint key)))
     (assert (s/valid? ::registered-compiler-key key)
             (str "Unregistered compile key: " (pr-str key)))
     (cond
       (or (= :hiccup from-format to-format)
           (not ((set [from-format to-format]) :hiccup)))
       (compile* doc opts)
       (= :hiccup from-format)
       (cond-> doc
         ;; check for tag-compilers before doing the hiccup -> hiccup conversions
         tag-compilers (compile* (merge opts {:from-format :hiccup :to-format :hiccup}))
         :always (compile* opts))
       (= :hiccup to-format)
       (cond-> doc
         :always (compile* opts)
         tag-compilers (compile* (merge opts {:from-format :hiccup :to-format :hiccup})))))))

(deftest ^:no-doc exercise-compile-args
  (is (s/exercise ::compile-args)))

(deftest ^:no-doc test-compile
  (is (rt/successful? (rt/check `compile {} {:num-tests 2}))))


(comment
  (compile
    {:data {:values [{:a 1 :b 2} {:a 3 :b 5} {:a 4 :b 2}]}
     :mark :point
     :encoding {:x {:field :a}
                :y {:field :b}}}
    {:from-format :vega-lite
     :to-format :pdf})
  :done-comment)


(defn- prep-for-live-view
  [doc {:keys [mode]}]
  [:div
    (if (map? doc)
      [(or mode :vega-lite) doc]
      ;; TODO These compilers should be considered a stopgap; Need to have markdown compilers for frontend as
      ;; well
      (compile-tags doc {:md       #(compile (second %) {:from-format :md :to-format :hiccup})
                         :markdown #(compile (second %) {:from-format :md :to-format :hiccup})
                         :pprint   (comp pprint-hiccup second)
                         :print    (comp print-hiccup second)}))
    mathjax-script])

(defonce ^:private last-viewed-doc
  (atom nil))

(defn view!
  "View the given doc in a web browser. Docs for which map? is true are treated as single Vega-Lite/Vega visualizations.
  All other values are treated as hiccup, and are therefore expected to be a vector or other iterable.
  This hiccup may contain Vega-Lite/Vega visualizations embedded like `[:vega-lite doc]` or `[:vega doc]`.
  You may also specify `:host` and `:port`, for server settings, and a `:mode` option, defaulting to `:vega-lite`, with `:vega` the alternate option.
  (Though I will note that Vega-Embed often catches when you pass a vega doc to a vega-lite component, and does the right thing with it.
  However, this is not guaranteed behavior, so best not to depend on it)"
  [doc & {:keys [host port mode] :as opts}]
  (let [port (or port (server/get-server-port) server/default-port)
        host (or host "localhost")]
    (try
      (prepare-server-for-view! port host)
      (let [hiccup-doc (prep-for-live-view doc opts)]
        ;; if we have a map, just try to pass it through as a vega form
        (reset! last-viewed-doc hiccup-doc)
        (server/send-all! [::view-doc hiccup-doc]))
      (catch Exception e
        (log/error "error sending plot to server:" e)
        (log/error "Try using a different port?")
        (.printStackTrace e)))))


(defmethod server/-event-msg-handler :oz.app/connection-established
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when-let [doc @last-viewed-doc]
    (let [session (:session ring-req)
          uid (:uid session)]
      (server/chsk-send! uid [::view-doc doc]))))

(defn ^:no-doc v!
  "Deprecated version of `view!`, which takes a single vega or vega-lite clojure map `viz`, as well as added `:data`,
  `:width` and `:height` options, to be merged into viz prior to `view!`ing."
  [viz & {:as opts
          :keys [data width height host port mode]
          :or {port (:port @server/web-server_ server/default-port)
               host "localhost"
               mode :vega-lite}}]
  ;; Update viz opts, then send view
  (let [viz (merge-opts viz opts)]
    (view! viz :host host :port port :mode mode)))



;; Publishing code

(defn- auth-args
  [args]
  (let [the-auth-args (submap args #{:auth :auth-token :client-id :access-token})
        auth-file (or (:auth-file args) (live/join-paths (System/getProperty "user.home") ".oz/github-creds.edn"))]
    (if (empty? the-auth-args)
      (try
        (edn/read-string (slurp auth-file))
        (catch Exception e
          (log/errorf "Unable to find/parse github authorization file `~/.oz/github-creds.edn`. Please review the output of `(doc oz/publish!)` for auth instructions.")
          (throw e)))
      the-auth-args)))

(defn gist!
  "Create a gist with the given doc

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
  * `:description`: auto generated based on doc"
  [doc & {:as opts
          :keys [name description public pretty]
          :or {public false}}]
  (let [type (doc-type doc)
        name (or name
               (case type
                 :ozviz "ozviz-document.edn"
                 :vega "vega-viz.json"))
        description (or description
                      (case type
                        :ozviz "Ozviz document; To load go to https://ozviz.io/#/gist/<gist-id>."
                        :vega "Vega/Vega-Lite viz; To load go to https://vega.github.io/editor"))
        doc-string (case type
                     :ozviz (if pretty
                              (with-out-str (clojure.pprint/pprint doc))
                              (pr-str doc))
                     :vega (if pretty
                             (json/generate-string doc {:pretty true})
                             (json/generate-string doc)))
        create-gist-opts (merge {:description description :public public}
                                (auth-args opts))
        gist (gists/create-gist {name doc-string} create-gist-opts)]
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
  "Publish doc via gist! and print out the corresponding vega-editor or ozviz.io url.

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
  * `:description`: auto generated based on doc
  * `:return-full-gist`: return the full tentacles gist api response data
  * `:pretty` : pretty print the published spec"
  [doc & {:as opts
          :keys [mode return-full-gist]
          :or {mode :vega-lite}}]
  (let [gist (apply-opts gist! doc opts)
        gist-url (:url gist)]
    (println "Raw gist url:" gist-url)
    (println "Gist url:" (:html_url gist))
    ;; Should really merge these into gist and return as data...
    (case (doc-type doc)
      :ozviz (println "Ozviz url:" (ozviz-url gist-url))
      :vega (println "Vega editor url:" (vega-editor-url gist :mode mode)))
    (when return-full-gist
      gist)))

(defn ^:no-doc publish-plot!
  "Deprecated form of `publish!`"
  [plot & opts]
  (log/warnf "WARNING!!! DEPRECATED!!! Please call `publish!` instead.")
  (let [doc (merge-opts plot opts)]
    (publish! doc opts)))


;; Refer to the live-reload! function
(clone-var live/live-reload!)
(clone-var live/kill-watcher!)
(clone-var live/kill-watchers!)


;; For the live-view! function below
(defn- view-file!
  [{:keys [host port format from-format]} filename context {:keys [kind file]}]
  ;; ignore delete (some editors technically delete the file on every save!
  (when (and (#{:modify :create} kind)
             (not (.isDirectory (io/file filename))))
    (let [contents (slurp filename)
          from-format (or from-format format (filename-format filename))]
      ;; if there are differences, then do the thing
      (when-not (= contents
                   (get-in @live/watchers [filename :last-contents]))
        (log/info "Rerendering file:" filename)
        (when-let [result
                   (if (#{:clj :cljc} from-format)
                     (live/reload-file! filename context {:kind kind :file file})
                     (load filename :from-format from-format))]
          (view! result :host host :port port))
        (swap! live/watchers assoc-in [filename :last-contents] contents)))))

(defn live-view!
  "Watch file for changes and apply `load` & `view!` to the contents"
  [filename & {:keys [host port format] :as opts}]
  (live/watch! filename (partial view-file! opts)))

(defn- drop-extension
  [relative-path]
  (string/replace relative-path #"\.\w*$" ""))

(defn- html-extension
  [relative-path]
  (string/replace relative-path #"\.\w*$" ".html"))


;; Building up to defining build

(s/def ::path string?)
(s/def ::from ::path)
(s/def ::to ::path)
(s/def ::template-fn
  (s/fspec :args (s/cat :input ::hiccup)
           :ret ::hiccup))
(s/def ::out-path-fn
  (s/fspec :args (s/cat :in-path ::path)
           :ret ::path))
(s/def ::as-assets? boolean?)

(s/def ::build-desc
  (s/keys :req-un [::from ::to]
          :opt-un [::out-path-fn]))

(s/def ::build-descs
  (s/coll-of ::build-desc))



(defn- compute-out-path
  [{:as build-desc :keys [from to out-path-fn]} path]
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

(defn- file-separator
  "This function returns the platform specific file separator
   and handles some platform specific issues as they arise.

   One particular issue is that the value returned by the File
   API on windows '\\' breaks the re-pattern function."
  []
  (let [separator (java.io.File/separator)]
    ;; Windows, replace with double escape.
    (string/replace separator "\\" "\\\\")))

(defn- ensure-out-dir
  [out-path drop-last?]
  (let [split-path (string/split out-path (re-pattern (file-separator)))
        split-path (if drop-last? (drop-last split-path) split-path)
        intermediate-paths (map-indexed
                             (fn [i _]
                               (string/join (file-separator) (take (inc i) split-path)))
                             split-path)]
    (doseq [path intermediate-paths]
      (let [file (io/file path)]
        (when-not (.isDirectory file)
          (.mkdir file))))))


(def ^:private supported-filetypes
  #{"md" "mds" "clj" "cljc" "cljs" "yaml" "json" "edn"})

(def ^:private asset-filetypes
  #{"jpg" "png" "svg" "css"})


;; For keeping track of builds

(defonce ^:private last-built-file (atom nil))
;@last-built-file


(def ^:private default-ignore-patterns
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
   {:as build-desc :keys [format from to out-path-fn template-fn html-template-fn as-assets? compile-opts]}
   filename context {:keys [kind file]}]
  (when (and from (.isDirectory (io/file from)))
    (ensure-out-dir to false))
  (if-let [ext (and file (extension (.getPath file)))]
    (cond
      ;; Handle asset case; just copy things over directly
      (or as-assets? (asset-filetypes ext))
      (when (and (not (.isDirectory file)) (#{:modify :create} kind) (not (ignore? file)))
        (let [out-path (compute-out-path (assoc build-desc :out-path-fn identity)
                                         (.getPath file))]
          (ensure-out-dir out-path true)
          (log/info "updating asset:" file)
          (io/copy file (io/file out-path))))
      ;; Handle the source file case
      (supported-filetypes ext)
      ;; ignore delete (some editors technically delete the file on every save!); also ignore dirs
      (when (and (#{:modify :create} kind) file (not (.isDirectory file)))
        (reset! last-built-file [(live/canonical-path file) build-desc])
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
                    (load filename :format format))]
              (when evaluation
                (let [;_ (log/info "step 1:" evaluation)
                      evaluation (with-meta (if template-fn (template-fn evaluation) evaluation) (meta evaluation))
                      ;_ (log/info "step 2:" evaluation)
                      out-path (compute-out-path build-desc filename)]
                  (ensure-out-dir out-path true)
                  (when view?
                    (log/info "Updating live view")
                    (view! evaluation :host host :port port))
                  (export! evaluation out-path)
                  (swap! live/watchers update filename (partial merge {:last-contents contents})))))))))))


(def ^:private default-config
  {:live? true
   :view? true
   :lazy? true
   :port 5760
   :host "localhost"})

(def ^:private default-build-desc
  {:out-path-fn html-extension})



(defn kill-builds!
  "Kills all file watchers"
  []
  ;; for now this should suffice
  (live/kill-watchers!))


(defn- infer-root-dir
  [build-descs]
  ;; not correct; mocking
  (->> build-descs
       (map (comp live/canonical-path :to))
       (reduce live/greatest-common-path)))


;; build config specs

(s/def ::live? boolean?)
(s/def ::lazy? boolean?)
(s/def ::view? boolean?)
(s/def ::root-dir ::path)


(s/def ::build-opts
  (s/keys :opt-un [::live? ::lazy? ::view? ::root-dir]))


(defn build!
  "Builds a static web site based on the content specified in specs. Each build-desc should be a mapping of paths, with additional
  details about how to build data from one path to the other. Available build-desc keys are:
    * `:from` - (required) Path from which to build
    * `:to` - (required) Compiled files go here
    * `:template-fn` - Function which takes Oz hiccup content and returns some new hiccup, presumably placing the content in question in some 
    * `:out-path-fn` - Function used for naming compilation output
    * `:to-format` - Literal format to use for export!
    * `:to-format-fn` - Function of input filename to format
    * `:as-assets?` - Pass through as a static assets (for images, css, json or edn data, etc)
      - Note: by default, images, css, etc will pass through anyway

  Additional options pertinent to the entire build process may be passed in:
    * `:live?` - Watch the file files 
    * `:lazy?` - If true, don't build anything until it changes; this is best for interactive/incremental updates and focused work.
                 Set to false if you want to rebuild from scratch. (default true)
    * `:view?` - Build with live view of most recently changed file (default true)
    * `:root-dir` - Static assets will be served relative to this directory (defaults to greatest-common-path between all paths)
  "
  ;; lazy? - (This is one that it would be nice to merge in at the build-desc level)
  ;; future: middleware?
  ([build-descs & {:as config}]
   (kill-builds!)
   (if (map? build-descs)
     (apply-opts build! [build-descs] config)
     (let [{:as full-config :keys [lazy? live? view?]}
           (merge default-config config)]
       (reset! server/current-root-dir (or (:root-dir config) (infer-root-dir build-descs)))
       (doseq [build-desc build-descs]
         (let [full-spec (merge default-build-desc build-desc)]
           ;; On first build, build out all of the results unless lazy? has been passed or we haven't built it
           ;; yet
           (doseq [src-file (file-seq (io/file (:from full-spec)))]
             ;; we don't want to display the file on these initial builds, only for most recent build
             (let [config' (assoc full-config :view? false)
                   dest-file (compute-out-path full-spec src-file)]
               (when (or (not lazy?) (not (.exists (io/file dest-file))))
                 (build-and-view-file! config' full-spec (:from full-spec) nil {:kind :create :file src-file}))))
           ;; Start watching files for changes
           (when live?
             (live/watch! (:from full-spec) (partial build-and-view-file! full-config full-spec)))))
       ;; If we're running this the second time, we want to immediately rebuild the most recently compiled
       ;; file, so that any new templates or whatever being passed in can be re-evaluated for it.
       (when-let [[file build-desc] @last-built-file]
         (let [new-spec (first (filter #(= (:from %) (:from build-desc)) build-descs))]
           (log/info "Recompiling last viewed file:" file)
           (build-and-view-file! full-config new-spec (:from build-desc) {} {:kind :create :file (io/file file)})))))))

;; for purpose of examples below
;; Or are they?

;; Some examples for you

(comment
  (defn- site-template
    [doc]
    [:div {:style {:max-width 900 :margin-left "auto" :margin-right "auto"}}
     doc])

  (defn- blog-template
    [doc]
    [site-template
      (let [{:as doc-meta :keys [title published-at tags]} (meta doc)]
        [:div
         [:h1 {:style {:line-height 1.35}} title]
         [:p "Published on: " published-at]
         [:p "Tags: " (string/join ", " tags)]
         [:br]
         doc])])

  ;; View a simple plot
  (view!
    [:div
      [:md "shiver me _timbres_"]
      [:vega-lite
        {:data {:values [{:a 1 :b 2} {:a 3 :b 5} {:a 4 :b 2}
                         {:a 2.3 :b 2.1} {:a 3.5 :b 5.7} {:a 3 :b 9}]}
         :mark :point
         :encoding {:x {:field :a :type :quantitative}
                    :y {:field :b :type :quantitative}}}]
      [:p "What do you think about that?"]])
    ;:port 10666)

  (view! [:vega-lite
          {:data {:values [{:x 1 :y 1}{:x 2.3 :y 2.1}{:x 3 :y 3}{:x 4 :y 4}{:x 5 :y 5}]}
           :transform [{:calculate "info(datum.x / datum.y)" :as "ratio"}]
           :mark {:type "line"}
           :encoding {:x {:field "x" :type "quantitative"}
                      :y {:field "y" :type "quantitative"}}}
          {:log-level :debug}]
         :port 10666)

  ;; View a more complex document
  (export!
    [:div
     [:h1 "Greetings, Earthling"]
     [:p "Take us to your King of Kings. We demand tribute!."]
     [:h2 "Look, and behold"]
     [:pprint {:yo :dawg}]
     [:vega-lite {:data {:values [{:a 2 :b 3} {:a 5 :b 2} {:a 7 :b 4}]}
                  :mark :point
                  :width 400
                  :encoding {:x {:field "a" :type :quantitative}
                             :y {:field "b" :type :quantitative}}}]]
    ; Should be using options for mode vega/vega-lite TODO
    "test.html")

  ;; Run live view on a file, and see compiled changes real time
  (kill-watchers!)
  (live-view! "examples/test.md" :port 10666)
  (live-view! "resources/oz/examples/clj/clj_test.clj")


  ;; Can kill file watchers and server manually if needed
  ;(kill-watchers!)
  ;(server/stop!)
  (server/start-server! 10666)

  ;; Run static site generation features
  (build!
    [{:from "examples/static-site/simple-static-site/src/site/"
      :to "examples/static-site/simple-static-site/build/"
      :template-fn site-template}
     ;; If you have static assets, like datasets or images which need to be simply copied over
     {:from "examples/static-site/simple-static-site/src/assets/"
      :to "examples/static-site/simple-static-site/build/"
      :as-assets? true}]
    :lazy? false
    :view? true
    :port 10666)
    ;:root-dir "examples/static-site/build")

  (clojure.walk/postwalk
    (fn [{:as elmt :keys [tag attrs]}]
      (if tag
        (update elmt :attrs merge attrs)
        elmt))
    {:tag "shit"
     :content
     [{:tag "gangsta"
       :content "yo"}]})

  :end-comment)


(def ^:no-doc help-message
  "args: command, &args
  command options
  ")

(defn -main
  [command & args]
  (case command
    "help" (println help-message)
    "build" (let [[build-spec & args'] args
                  build-spec (edn/read-string build-spec)] 
              (println "build-spec" (pr-str build-spec))
              (println "args'" args')
              (build! build-spec)
              (println "done calling build"))))
              ;(a/<!! (a/chan)))))

