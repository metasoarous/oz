(ns oz.next
  (:require ;[clojure.tools.analyzer :as ana]
            ;[clojure.tools.analyzer.env :as env]
            [taoensso.timbre :as log]
            [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [markdown-to-hiccup.decode :as md-decode]
            [markdown-to-hiccup.core :as md->hc]
            [clojure.test :as test :refer [testing deftest is]]
            [hickory.core :as hickory]
            [clojure.tools.reader :as reader]
            ;[rewrite-clj.zip :as rwz]
            [parcera.core :as parc]
            ;[clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [clojure.walk :as walk]
            [clojure.set :as set]
            ;[oz.core :as core]
            [clojure.java.io :as io]
            [markdown.core :as md]
            [hasch.core :as hasch]
            [oz.live :as live]
            [oz.server :as server]
            [oz.impl.utils :as utils]
            [clojure.tools.analyzer.jvm :as ana.jvm]
            [clojure.core.async :as async :refer [go go-loop <! >! <!! >!!]]))



(def types
  #{:code :hiccup :md-comment :whitespace :code-comment}) 
(s/def ::type types)

(def evaluable-types
  #{:code :hiccup :md-comment :whitespace :code-comment})
(s/def ::evaluable-type evaluable-types)


(s/def ::block
  (s/keys :req-un [:oz.block/id]))



;(s/def ::evaluable-block
  ;(s/and ::block
         ;(comp (s/valid?))))
  


;(require '[clojure.tools.namespace.repl :refer [refresh]])
;(refresh)


; This is a comment that shouldn't be converted
;(parc/ast (slurp "test.clj"))
;(parc/ast "#(f %) (fn [x] x)")
;(parc/code (parc/ast "#(f %) (fn [x] x)"))
;(parc/failure? (nth (parc/ast "(+ 5 6) (+ 3 4") 3))
;(parc/code
  ;(second
    ;(parc/ast (slurp "test.clj"))))

;(parc/ast ";; this \n\n ;;that")
;(parc/code [:code [:list [:symbol "f"]]])
;(parc/code [:code (second (parc/ast "(f)"))])
;(let [ast (parc/ast "(f)")]
  ;(meta ast)) 
  ;(parc/code (with-meta (:code (rest ast))
                        ;(meta ast)))) 

;(parc/ast "(defn f [x]\n  ;;some code...\n(* x 2))")
;(meta (parc/default-hidden (parc/ast "(defn f [x]\n  ;;some code...\n(* x 2))")))

(def md-comment-regex
  #"^;;( (.*))?")
;(re-matches #"^;;( (.*))?" ";; This is the stuff that matters")

(defn- get-comment-line-md [comment-line]
  (or (last (re-matches md-comment-regex comment-line)) ""))

;(get-comment-line-md ";; this is a line!")

(def trailing-spaces-regex
  #"\s*[\n\r][ \t]+$")

; should match
;(re-matches trailing-spaces-regex "\n  ")
;(re-matches trailing-spaces-regex "\n\t")
;(re-matches trailing-spaces-regex "\n \n  ")

; should NOT match
;(re-matches trailing-spaces-regex "\n")
;(re-matches trailing-spaces-regex "\n \n")
;(re-matches trailing-spaces-regex "\n \n")


(defn unindented-newline?
  [last-form]
  ;; main condition: preceded by a whitespace line, without any indendation
  (or (and (= :whitespace (first last-form))
           (not (re-matches trailing-spaces-regex (second last-form))))
      ;; to handle the edge case of first line in a file being a comment line
      (nil? last-form)))


(def comment-metadata-regex
  ;#"^;; (\^[(:[^\s]+)(\{.*\})])+"
  #";; \^(:\S+|\{.*\})")

;(re-matches comment-metadata-regex ";; ^:this")
;(re-find #";; (\^(:\S+|\{.*\})\s+)+" ";; ^{:this :blah} ^{:other :stuff}")
;(re-matches #";; \^(:\S+|\{.*\})" ";; ^:this")
;(re-matches #";; \^(:\S+|\{.*\})" ";; ^{:this :that}")


(defn form-type
  [last-form form]
  (case (first form)
    :comment (cond
               ;; metadata annotations are (for now) only recognized between the 
               (and (re-matches md-comment-regex (second form))
                    (unindented-newline? last-form))
               :md-comment
               :else
               :code-comment)
    :whitespace :whitespace
    ;; Do we do it like this?
    :vector :hiccup
    :code))

(form-type
  [:whitespace "  \n\n"]
  [:comment ";; ^:this"])
  ;(re-matches comment-metadata-regex ";; ^:this"))

(defn multiline-whitespace?
  [[type code-str]]
  ;; Check for whitespace, and that it's just a single newline
  (and (= :whitespace type)
       (< 1 (count (re-seq #"\n" code-str)))))

(multiline-whitespace? [:whitespace " \n \n"])
(multiline-whitespace? [:whitespace " \n"])

;; FOCUS
(defn- new-block?
  [{:keys [last-form block-type]} next-form]
  (let [next-form-type (form-type last-form next-form)]
    (boolean
      (or
        ;; always end on a code block, unless it's just a trailing comment
        ;(and (#{:code :}))
        (multiline-whitespace? next-form)
        (and (#{:code :hiccup} block-type)
             (= :code-comment next-form-type))
        ;; markdown blocks can only be completed by more markdown blocks or whitespace
        (and (= :md-comment block-type)
             (not (#{:md-comment :whitespace} next-form-type)))
        ;; code comments can only be followed by more code-comments, whitespace or code
        (and (= :code-comment block-type)
             (not (#{:code-comment :whitespace :code :hiccup} next-form-type)))
        ;; we separate out leading whitespace, so as to not clutter code blocks with it
        (and (= :whitespace block-type)
             (not= :whitespace next-form-type))))))

(defn- set-last-form
  [aggr last-form]
  (assoc aggr :last-form last-form))

(defn read-meta-comment
  [md-string]
  (let [meta-line (first (string/split md-string #"\n"))]
    (try
      (meta
        (reader/read-string
          (str (apply str (drop 2 meta-line))
              " {}")))
      (catch Throwable t
        (log/error "Unable to read metadata from: " meta-line)
        nil))))

;; Defines how we add code to a block
(defn- add-to-current-block
  [{:as aggr :keys [last-form block-type]} next-form]
  (let [next-form-type (form-type last-form next-form)
        block-type (cond
                     ;; adding whitespace shouldn't change the type if already set
                     (= next-form-type :whitespace)
                     (or block-type :whitespace)
                     ;; we can add code comments to the end of code/hiccup blocks
                     (and (#{:hiccup :code} block-type)
                          (= next-form-type :code-comment))
                     (or block-type :code-comment)
                     ;; otherwise we take the new form type, assuming prior was deferential
                     :else
                     next-form-type)]
    (-> aggr
        (update :current-block conj next-form)
        (assoc :block-type block-type)
        (set-last-form next-form))))

;; TODO Do we need async vlaues for the metadata for this to work?

(defn- conclude-block
  [{:as aggr :keys [current-block block-type]}]
  (if current-block
    ;; only conclude a block if one actually exists
    (-> (dissoc aggr :block-type :current-block)
        (update :blocks conj {:type block-type :forms current-block}))
    aggr))

(defn- start-new-block
  [{:as aggr :keys [last-form]} next-form]
  (-> (conclude-block aggr)
      (assoc :current-block [next-form]
             :block-type (form-type last-form next-form))
      (set-last-form next-form)))

(defn- reconstitute-forms
  [{:as block :keys [forms]}]
  (assoc block :code-str (parc/code (into [:code] forms))))

(defn- apply-metadata
  [[tag first-elmnt & rest-elmnts] metadata]
  ;(log/info "QQQQQQ calling apply-metadata with meta: " meta " and hiccup: " hiccup)
  (into
    [tag
     (if (map? first-elmnt)
       (merge first-elmnt metadata)
       metadata)
     (when-not (map? first-elmnt)
       first-elmnt)]
    rest-elmnts))


(defn- strip-metadata-if-needed
  [meta? lines]
  (if meta?
    (drop 1 lines)
    lines))

(defn- unwrap-single-child-div
  [[tag & forms :as form]]
  (let [attr-map (when (map? (first forms)) (first forms))
        non-attr-forms (cond->> forms
                         attr-map (drop 1))]
    (if (and (= :div tag)
             (= 1
                (count non-attr-forms)))
      ;; TODO Do we need to merge meta here?
      (-> (first non-attr-forms)
          (apply-metadata attr-map))
      form)))

(defn has-metadata?
  [code-str]
  (boolean (re-matches comment-metadata-regex (first (string/split-lines code-str)))))

(has-metadata? ";; ^{:this :that}\n;; more stuff")

(defn- process-md-comments
  [{:as md-block :keys [code-str]}]
  (let [meta? (has-metadata? code-str)
        markdown
        (->> (string/split-lines code-str)
             (strip-metadata-if-needed meta?)
             (map get-comment-line-md)
             (string/join "\n"))
        {:keys [metadata html]}
        (md/md-to-html-string-with-meta markdown)
        metadata (merge metadata (when meta? (read-meta-comment code-str)))
        ;;     parse the html as hiccup
        hiccup (-> html hickory/parse hickory/as-hiccup first
                   ;; not sure why we do this actually
                   md->hc/component md-decode/decode
                   unwrap-single-child-div
                   (cond->
                     ;; we only apply the metadata if there's any to apply
                     (seq metadata) (apply-metadata metadata)))]
    (assoc md-block
           :markdown markdown
           :html-string html
           :hiccup hiccup)))

(defn- without-whitespace
  [forms]
  (walk/prewalk
    (fn [form]
      (if (coll? form)
        (remove
          #(when (coll? %)
             (= :whitespace (first %)))
          form)
        form))
    forms))

(defn parse-code
  [code-str]
  (->> (parc/ast code-str)
       ;; drop the :code demarkation
       rest
       (reduce
         (fn [aggr next-form]
           (if (new-block? aggr next-form)
             (start-new-block aggr next-form)
             (add-to-current-block aggr next-form)))
         {:blocks []})
       (conclude-block)
       :blocks
       (map
         (comp
           reconstitute-forms
           (fn [{:as block :keys [forms]}]
             (assoc block :forms-without-whitespace (without-whitespace forms)))))))


;; TODO Write tests
;(parse-code
 ;"
;(ns my.ns)\n \n(defn some-code [x]\n  (impl x))\n \n  ;(other-iml x))\n\n;; Actual markdown comments.\n
 ;")

;(parse-code
 ;"
;(ns my.ns)\n \n(defn some-code [x]\n  (impl x))\n  ;(other-iml x))\n\n;; Actual markdown comments.\n
 ;")

(deftest parse-code-tests
  (testing "basic functionality"
    (is (seq?
          (parse-code "(ns my.ns)\n(def thing :stuff)")))))
                    

;(parse-code (slurp "test.clj"))
;(defined-vars)


;(let [val (into {} (doall (map vec (partition 2
                                              ;(interleave (range 1000000)
                                                          ;(range 1000000))))))]
  ;(time (hash val)))


;core/hash -> 0.3s
;uuid -> 1.5s
;b64-hash -> 1.5s


;(ana.jvm/analyze '(def shit "yo"))
;(ana.jvm/analyze '(defmulti shit "yo"))
;(ana.jvm/analyze '(defmulti shit "yo"))

;(ana.jvm/analyze '(def a-number 4))
;(ana.jvm/analyze '(+ a-number 4))
;(ana.jvm/analyze '(do (def form []) (ana.jvm/analyze form)))
;a-number
;(+ a-number 4)


(defn analysis-zipper [analysis]
  (zip/zipper
    (fn [n] (map? n))
    (fn [n] (->> (dissoc n :env)
                 (filter (fn [[k v]]
                           (and (not (#{:env :form :arglists :raw-forms} k))
                                (coll? v))))
                 (mapcat (fn [[_ v]]
                           (if (sequential? v) v [v])))
                 (filter coll?)
                 (remove nil?)))
    (fn [n _] n)
    analysis))

(defn zipper-nodes [zip]
  (->> zip
       (iterate zip/next)
       (take-while #(not (zip/end? %)))
       (map zip/node)))

(defn analysis-nodes
  [form]
  (->> form ana.jvm/analyze analysis-zipper zipper-nodes))


;(comment
  ;(->>
    ;(analysis-nodes '(let [v (def shit "yo")] (println "defined" v)))
    ;(filter (fn [n] (= :def (:op n))))
    ;(map :var))

  ;(->>
    ;(analysis-nodes '(do (println "stuff" clojure.core/map)))
    ;(filter #(and (:var %) (not= (:op %) :def)))
    ;(map :var))

  ;(->>
    ;(analysis-nodes '(defmethod shit :stuff [args] (println args)))
    ;(filter #(and (:var %) (not= (:op %) :def)))
    ;(map :var))
    ;;(map :var))

  ;(->>
    ;(analysis-ndoes '^{:stuff map}[3 4])
    ;(filter #(and (:var %) (not= (:op %) :def)))
    ;(map :var)))


;(:body (ana.jvm/analyze '(let [v (def shit "yo")] (println "defined" v))))

;(ana.jvm/analyze 
  ;^{:with (+ 3 4)}[])

;(ana.jvm/analyze '(with-meta [] {:meta :stuff}))
;(ana.jvm/analyze '(swap! shit merge {:stuff :yeah}))
;(ana.jvm/analyze '(swap! (:stuff shit) merge {:stuff :yeah}))
;(ana.jvm/analyze '(reset! shit {:stuff :yeah}))
;(ana.jvm/analyze '(defmethod shit :stuff [args] (println args)))


(defn var-form? [n]
  (and (:var n)
       (not= (:op n) :def)))
  
(defn var-def-form? [{:keys [op]}]
  (= :def op))


(declare shit)
(deftest var-def-analysis-test
  (let [anal-node (first (analysis-nodes '(def shit "yo")))]
    (is (= true (var-def-form? anal-node)))
    (is (= (var oz.next/shit) (:var anal-node)))))


(defn addmethod-mutation? [{:keys [method]}]
  (= 'addMethod method))

(defn addmethod-var [{:keys [instance]}]
  (:var instance))


(deftest defmethod-analysis-test
  (let [anal-node
        (first (analysis-nodes '(defmethod shit :stuff [args] (println args))))]
    (is (= true (addmethod-mutation? anal-node)))
    (is (= (var oz.next/shit) (addmethod-var anal-node)))))

(defn mutated-atom-var? [{:as n :keys [op args]}]
  (and (= :invoke op)
       (#{#'clojure.core/swap! #'clojure.core/reset!} (:fn n))
       (= :var (:op (first args)))))

(defn mutated-atom-var [{:keys [args]}]
  (-> args first :var))


(defn -resolve-var-lists
  [defines]
  (->> (if (coll? defines)
         defines
         [defines])
       (map
         #(if (var? %) 
            %
            ;; find or declare the var if a symbol
            (or (find-var %) (eval (list 'declare %)))))))

(defn explicit-mutation [{:keys [op meta]}]
  (when (= :with-meta op)
    (when-let [mutates (:oz.block/mutates meta)]
      (-resolve-var-lists mutates))))

(defn explicit-dependencies [{:keys [op meta]}]
  (when (= :with-meta op)
    (when-let [depends (:oz.block/depends-on meta)]
      (-resolve-var-lists depends))))

(defn explicit-defines [{:keys [op meta]}]
  (when (= :with-meta op)
    (when-let [defines (:oz.block/defines meta)]
      (-resolve-var-lists defines))))

;(let [s `this] (or (find-var s) (eval '(declare ~s))))

(->>
  ;(ana.jvm/analyze '(defmethod shit :stuff [args] (println args)))
  ;(ana.jvm/analyze '(let [v (def shit "yo")] (println "defined" v)))
  (analysis-nodes '(def shit "yo"))
  (first))
  ;(map addmethod-mutation?))

(defn- set-conj
  [xs x]
  (conj (set xs) x))

(defn ns-form-ns
  [code-data]
  (and (list? code-data)
       (= 'ns (first code-data))
       (symbol? (second code-data))
       (second code-data)))


(ns-form-ns '(ns this.that))

(defn analyze-block
  [{:as block :keys [code-str]}]
  (let [code-data (reader/read-string code-str)
        anal-nodes (analysis-nodes code-data)] 
    (merge block
      (reduce
        (fn [anal-results node]
          (cond-> anal-results
            (var-def-form? node)       (update :defined-vars set-conj (:var node))
            (var-form? node)           (update :used-vars set-conj (:var node))
            (mutated-atom-var? node)   (update :mutated-vars set-conj (mutated-atom-var node))
            (addmethod-mutation? node) (update :mutated-vars set-conj (addmethod-var node))))
        (merge
          {:code-data code-data
           :defined-vars (explicit-defines block)
           :used-vars (explicit-dependencies block)
           :mutated-vars (explicit-mutation block)}
          (when-let [ns-sym (ns-form-ns code-data)]
            {:declares-ns ns-sym}))
         ;:analysis (first anal-nodes)}
        anal-nodes))))



;(nth
  ;;(analysis-nodes '(do (def shit "dawg") (println shit)))
  ;(analysis-nodes '(let [v (def shit "yo")] (println v)))
  ;0)

;(try
  ;(analyze-block {:code-str '(let [v (def shit "yo")] (println v))})
  ;;(analyze-block {:code-str '(do (def shit "dawg") (println shit))})
  ;(catch Throwable t
    ;(.printStackTrace t)))
;(map :meta (:defined-vars (analyze-block {:code-data '(defmulti shitterz first)})))

(deftest analyze-block-test
  (testing "simple def"
    (let [anal (analyze-block {:code-str (str '(def shit "yo"))})]
      (is (= #{(var oz.next/shit)}
             (:defined-vars anal)))))
  (testing "nested def"
    (let [anal (analyze-block {:code-str (str '(do (def shit "yo") (println shit)))})]
      (is (= #{(var oz.next/shit)}
             (:defined-vars anal))))
    (let [anal (analyze-block {:code-str (str '(let [v (def shit "yo")] (println v)))})]
      (is (= #{(var oz.next/shit)}
             (:defined-vars anal)))))
  (testing "defmulti"
    (let [anal (analyze-block {:code-str (str '(defmulti shit first))})]
      (is (= #{(var oz.next/shit)}
             (:defined-vars anal)))))
  (testing "var usage"
    (let [anal (analyze-block {:code-str (str '(str shit "dude"))})]
      (is (= true (contains? (:used-vars anal)
                             (var oz.next/shit)))))))


;; Inferring dependencies....

;; For each code block, we compute a hash based on:
;; * the :forms-without-whitespace representation
;; * the hash for each block it depends on, based on
;;   * the block precedes, and
;;   * either defines or mutates a used var

;; As we do this, we cache the forms 

;; This implies that we need to be able to:
;; * quickly map vars to blocks which mutate those vars

;; We may not actually have to explicitly compute dependencies...
;; When we're ready to execute, we can simply iterate through the blocks and for each:
;; * look at the vars they use
;; * get the forms 

;(meta #'hasch.core/uuid)
;(require '[clojure.java.io :as io])
;(.getPath (io/resource "hasch/core.cljc"))
;(.getPath (io/resource "oz/next.clj"))
;(io/resource "oz/next.clj")

(def local-file?
  (memoize
    (fn [filename]
      (let [path (.getPath (io/resource filename))]
        (string/starts-with? path (System/getProperty "user.dir"))))))
;(local-file? "oz/next.clj")
;(local-file? "hasch/core.cljc")

(defn local-var?
  [v]
  (if-let [{:keys [file]} (meta v)]
    (local-file? file)
    true))

(defn block-hash
  "Computes a hasch using a blocks forms without whitespace, as well as all of its dependencies"
  [block]
  (hasch/uuid
    (select-keys block [:forms-without-whitespace :dependencies :other-dependencies])))


;; debug the state of a file
;(doseq [[block-id result-chan]
        ;(:result-chans (:last-evaluation (get @evaluation-state "/home/csmall/code/oz/notebook-demo.clj")))]
  ;(when-let [{:as result :keys [error aborted]} (<!! result-chan)]
    ;(when (or error aborted)
      ;(log/info "block: " block-id)
      ;(log/info result))))

;(parse-code (slurp "test.clj"))

;; Need to make it so thast upstream changes to vars will update the functions in question

;(defn shit []
  ;:yeah)

;(meta (var shit))

;(clojure.repl/source-fn 'shit)



;; * go through entire set of vars used
;; * get all local files referenced that aren't the file in question
;; * either
;;   * read these files/vars lazily, as needed
;;     * this implies that evaluations need to be more unified between different files
;; * how do you make this interoperable with watching these files explicitly?

;; * what happens when a build file is saved?
;; * what happens if a dependency file is saved?



;; ideals:
;; * q: what happens when a dependency is changed:
;;   * a: everything downstream updates, and last viewed file is updated & displayed
;; * q: 


;; or

;; just assume that users always explictily watch all the implied directories?
;;   I don't like this solution; feels like a cop out


(meta (the-ns 'clojure.core))
(meta (the-ns 'clojure.set))
;(find-ns-definition 'oz.core); (find-ns 'oz.core))


(defn- get-other-dependencies
  [var-list]
  (mapcat
    (fn [v]
      (when-let [m (meta var-list)]
        (when-let [f (:file m)]
          (when (local-file? f)
              [{:file f
                :var v}]))))))


(defn- get-upstream-dependencies
  [var-list]
  (->> var-list
    (mapcat
      (fn [v]
        (when-let [m (meta var-list)]
          (when-let [f (:file m)]
            (when (local-file? f)
                [{:file f
                  :var v}])))))))


;; things we know:

;; * we want to be able to save a dependency file and have it run the updates
;;   * we need to have downstream targets or downstream dependencies attributes on the evaluation
;;     * 2 evaluations should always be able to reference the previous evaluation from another update
;;   * meta evaluation
;;   * need to have a model of how nss relate to eachother
;; * evaluations refer to other evaluations
;; * need to have some pause for a series of file updates to stop before we make any decisions
;;   * queue of incoming

;; the key is in processing batches of file changes with a timeout between signals
;; * like the 3-seconds popcorn popper rule

;; so we need a go loop that 

;; we take our watch fn and have it spit out updates into a rather massively buffered queue
;; * we kind of don't want to have builds run with incomplete file sets, since they could lead to inconsistent builds
;;   * then again... we may not want to allow builds of any size, since it could crap out an attempt to compute the batch, and we could potentially make it batch things, though without bakpressure, this will crumble

;; we start a go loop that watches for changes on the queue, and once a certain amount of time has passed between inbound updates, will initiate a build with the give set of caught files as the focus of the build

;; What do we want to call this set?
;; * focus-set
;; * 

;; this set of ids is what anchors our build
;; they are the things we're primarily watching
;; primary-watch-set of
;; primary-watch-file


(require '[nextjournal.beholder :as beholder])

;(beholder/stop example-watcher)

;(defn build-log-fn []
  ;(let [t0 (System/currentTimeMillis)]
    ;(fn [value]
      ;(println "value:" value)
      ;(println "t=" (- (System/currentTimeMillis) t0) "ms"))))

;(def example-watcher (beholder/watch (build-log-fn) "src"))

;(def emacs-swap-regex #"^(\.#.*|#.*)")
;;(re-matches emacs-swap-regex ".#stuff-file")
;;; ^ truthy

;(def vim-swap-regex #"^.+?\.sw.?$")
;(def vim-swap-regex #"^.+?\.sw.?$")
;(re-matches vim-swap-regex ".stuff-file.clj.swo")
;;; ^ truthy
;(re-matches vim-swap-regex "stuff-file.clj.swp"
;;; ^ not
;(re-matches
   ;#"^\d*$"
   ;"33234398734873")
   ;;#"^\..*\.sw\wx?$"
            ;;".stuff-file.clj.swp")


(def ^:private default-ignore-patterns
  [#".*~$"
   #"^\d*$"
   #"^(\.#.*|#.*)"
   #"^\..*\.sw\wx?$"])

(defn- ignore?
  [ignore-patterns f]
  (some
    #(-> f io/file .getPath (string/split #"\/") last (->> (re-find %)))
    ignore-patterns))


;; `watch-files!` starts the watch process and returns an object which specifies the files that need to be watched, and what needs to happen to them

(defonce current-watcher
  (atom nil))


(defn- kill-watcher!
  [{:keys [watchers update-batch-chan update-chan]}]
  (async/close! update-batch-chan)
  (async/close! update-chan)
  (doseq [[_ ws] watchers]
    (doseq [w ws]
      (beholder/stop w))))

(defn build-watch-fn
  [update-chan {:as build-spec :keys [ignore-file-patterns] :or {ignore-file-patterns default-ignore-patterns}}]
  (fn [{:as file-update :keys [path]}]
    (when (not (ignore? ignore-file-patterns (str path)))
      (>!! update-chan (assoc file-update :build-spec build-spec)))))

(defn batch-updates
  [{:keys [update-chan update-batch-chan]}]
  (go-loop [updates-so-far []]
    (let [timeout-chan (async/timeout 100)
          [result result-chan] (<! (async/alts! [update-chan timeout-chan]))]
      (if (not= result-chan timeout-chan)
        ;; If we're not at the timeout, just keep building
        (recur (conj updates-so-far result))
        ;; If we are at the timeout, what we do depends on whether we have a set of file changes or not
        (if (seq updates-so-far)
          (do
            (>! update-batch-chan updates-so-far)
            (recur updates-so-far))
          (recur updates-so-far))))))

(defn watch-files!
  "starts the watch process and returns an object which specifies the files that need to be watched, and what needs to happen to them"
  [build-specs]
  (let [watcher
        (reduce
          (fn [{:as watcher :keys [update-chan]}
               {:as build-spec :keys [from]}]
            (let [watch-handle (beholder/watch
                                 (build-watch-fn update-chan build-spec)
                                 from)]
              (-> watcher
                  (update :watchers
                          update from (comp set conj) watch-handle))))
          {:update-chan (async/chan 10000)
           :update-batch-chan (async/chan 10)}
          build-specs)]
    (swap! current-watcher
           (fn [last-watcher]
             (kill-watcher! last-watcher)
             watcher))))



(defn compress-updates
  [update-batch]
  (->> update-batch
       (group-by #(select-keys % [:build-spec :path]))
       (map
         (fn [[compressor updates]]
           (reduce
             (fn [{:as compressor :keys [update-type]}
                  {:keys [type path]}]
               ;; This catch cases where an editor deletes a file and then recreates it tens of ms later (stares at vim)
               (let [update-type (if (and update-type
                                          (= :create type))
                                   :modify
                                   type)]
                 (assoc compressor :update-type update-type)))
             compressor
             updates)))
       (group-by :path)
       (map (fn [[path updates]]
              {:path path
               ;; This is weird, but we've got the potential for multiple build spec entries matching a particular file
               ;; (even using the same :from declaration, which we don't properly handle above)
               :update-type (-> (map :update-type updates)
                                (set)
                                ;; if ambiguous, default order of update-type assumption is as below
                                ;; this could probably be refined/improved, but _shouldn't_ matter as long as we're not hitting timeouts
                                (some [:modify :create :delete]))
               :build-specs (->> (map :build-spec updates) set)}))))

;(compress-updates
  ;[{:type :delete
    ;:path "this/that.clj"
    ;:build-spec {}}
   ;{:type :create
    ;:path "this/that.clj"
    ;:build-spec {}}
   ;{:type :create
    ;:path "this/that.clj"
    ;:build-spec {:stuff :yeah}}
   ;{:type :create
    ;:path "this/those.clj"
    ;:build-spec {:stuff :yeah}}])


;(defn build-dependency-watch-fn
  ;[update-chan build-spec primary-watch-file]
  ;(let []))


;(defn add-dependency-watch!
  ;[build-spec primary-watch-file dependency-watch-file]
  ;(swap! current-watcher
         ;(fn [watcher]
           ;(let [watch-handle (beholder/watch
                                ;(build-watch-fn update-chan build-spec)
                                ;from)]
             ;(update-in watcher [:watchers dependency-watch-file] watch-handle)))))




;(defn file-watcher
  ;(async/go-loop []))

;; we need to replace the notion of the :evaluation with that of the `:evaluation-set`
;; an :evaluation corresponds with a single file
;; an :evaluation-set corresponds with a set of files (and thus :evaluations)
;; each :evaluation-set has a "head" of files that are being explicitly watched (:explicitly-watched-files ?)
;; * need to be able to map back towards corresponding build specifications in order to infer this
;;   * this changes over time
;;     * means probably need to track this as we're building our :evaluations
;; 



; See if we can take this to get the source for dependent vars
(defn infer-dependencies
  [blocks]
  (->> blocks
       (reduce
         (fn [{:keys [var-mutation-blocks block-id-seq blocks-by-id ns-sym]}
              {:as block :keys [defined-vars mutated-vars used-vars declares-ns]}]
           (let [dependency-vars (set/difference
                                   (set (into used-vars mutated-vars))
                                   defined-vars)
                 block (assoc block
                              :dependencies (apply set/union
                                                   (map var-mutation-blocks dependency-vars))
                              :other-dependencies (get-other-dependencies dependency-vars))
                 id (block-hash block)
                 block (assoc block
                              :id id
                              ;; we always evaluate from the most recent ns
                              :ns-sym ns-sym)
                 new-var-mutations
                 (reduce
                   (fn [var-block-mapping v]
                     (update var-block-mapping v set-conj id))
                   var-mutation-blocks
                   (concat defined-vars mutated-vars))]
             ;; if this is a ns declaration form, set ;ns-sym for next reduce iteration
             {:ns-sym (or declares-ns ns-sym)
              :var-mutation-blocks new-var-mutations
              :block-id-seq (conj block-id-seq id)
              :blocks-by-id (assoc blocks-by-id id block)}))
         {:ns-sym 'user
          :block-id-seq []
          :var-mutation-blocks {}
          :blocks-by-id {}})))

(defn process-blocks
  [blocks]
  (->> blocks
       (map (fn [{:as block :keys [type]}]
              (case type
                :md-comment (process-md-comments block)
                (:code :hiccup) (analyze-block block)
                (:whitespace :code-comment) block)))))

(defn block-seq
  [{:keys [block-id-seq blocks-by-id]}]
  (->> block-id-seq
       (map (fn [id] (get blocks-by-id id)))))


(deftest infer-dependencies-tests
  (testing "returns a ns-symbol, which will be the first such in the file"
    (let [get-nss (fn [code-str]
                    (->> code-str
                         parse-code
                         process-blocks
                         infer-dependencies
                         (block-seq)
                         (filter (comp #{:code} :type))
                         (map :ns-sym)))]
      (testing "basic functionality"
        (is (= '[user my.ns]
               (get-nss "(ns my.ns)\n(def thing :stuff)")))))))

(defn spy-pp
  ([message xs]
   (log/info message (with-out-str (pp/pprint xs)))
   xs)
  ([xs] (spy-pp "" xs)))

(defn analysis
  [code-str]
  (->> (parse-code code-str) process-blocks infer-dependencies))


(deftest dependency-test
  (testing "simple dependency"
    (let [code-str "(def stuff :blah)\n(str stuff \"dude\")"
          analysis-results (analysis code-str)
          code-blocks (->> (block-seq analysis-results)
                           (filter #(= :code (:type %))))]
      (is (= #{} (-> code-blocks first :dependencies)))
      (is (= true
             (contains? (-> code-blocks second :dependencies)
                        (-> code-blocks first :id)))))))


(defn block-token
  [id]
  [:oz.doc/block id])

(defn apply-template
  [template-fn {:as analysis-results :keys [block-id-seq]}]
  (assoc analysis-results
         :template (template-fn (map block-token block-id-seq))))

;(defn publish-result [block result])

(defn log-long-running-form!
  [{:keys [result-chan timeout-chan error-chan long-running?]}
   {:keys [code-str id]}]
  (async/go
    (let [[_ chan] (async/alts! [result-chan timeout-chan error-chan])]
      (when (= chan timeout-chan)
        (log/info (utils/color-str utils/ANSI_YELLOW "Long running form (" id ") being processed:\n" code-str))
        (async/>! long-running? true)))))

;(try
  ;(/ 1 0)
  ;(catch Throwable t
    ;{:message (str t)
     ;:stacktrace (with-out-str (clojure.stacktrace/print-stack-trace t))}))

(defn error-data [t]
  {:message (str t)
   :stacktrace (with-out-str (clojure.stacktrace/print-stack-trace t))})

(defn handle-error!
  [{:as evaluation :keys [t0 result-chan error-chan long-running? new-form-evals t0]}
   {:as block :keys [code-str id]}
   t]
  (let [compute-time (/ (- (System/currentTimeMillis) t0) 1000.0)]
    (async/>!! result-chan {:id id
                            :compute-time compute-time
                            :error (error-data t)
                            :result t})
    ;(log/error (utils/color-str utils/ANSI_RED "Error processing form (" id "):\n" code-str))
    (log/error (str "Error processing form (" id "):\n" code-str))
    (log/error t)
    (async/>!! error-chan t)
    (throw t)))

(defn handle-result!
  [{:as evaluation :keys [t0 result-chan long-running? new-form-evals t0]}
   {:as block :keys [id]}
   result]
  (let [compute-time (/ (- (System/currentTimeMillis) t0) 1000.0)]
    (async/>!! result-chan (merge result
                                  {:id id
                                   :compute-time compute-time}))
    ;; keep track of successfully run forms, so we don't redo work that completed
    ;; If long running, log out how long it took
    (when (async/poll! long-running?)
      (println (utils/color-str utils/ANSI_YELLOW "Form (" id ") processed in: " compute-time "s")))))

(defn init-block-evaluation [main-evaluation]
  (merge main-evaluation
    {:result-chan (async/promise-chan)
     :timeout-chan (async/timeout 1000)
     :long-running? (async/promise-chan)
     :error-chan (async/promise-chan)}))

(defn all-complete?
  [dependency-chans]
  (async/go
    (doseq [c dependency-chans]
      (<! c))
    :done))

(defn complete-results
  [dependency-chans]
  (async/go-loop [results []
                  chans dependency-chans]
    (when-let [chan (first chans)]
      (let [result (<! chan)
            results (conj results result)]
        (if (empty? (rest chans))
          results
          (recur results (rest chans)))))))

(defn ns-form-references
  "Separate out all of the 'reference forms' from the rest of the ns declaration; we'll use this in a couple
  different ways later"
  [ns-form]
  (let [reference-forms (->> ns-form (filter seq?) (map #(vector (first %) (rest %))) (into {}))]
    (concat
      ;; We need to use explicit namespacing here for refer-clojure, since hasn't been
      ;; referenced yet, and always include this form as the very first to execute, regardless of whether it shows
      ;; up in the ns-form, otherwise refer, def, defn etc won't work
      [(concat (list 'clojure.core/refer-clojure)
               (:refer-clojure reference-forms))]
      ;; All other forms aside from :refer-clojure can be handled by changing keyword to symbol and quoting
      ;; arguments, since this is the corresponding call pattern from outside the ns-form
      (map
        (fn [[k v]]
          (concat (list (-> k name symbol))
                  (map #(list 'quote %) v)))
        (dissoc reference-forms :refer-clojure)))))


(defmacro result-with-out-str
  "Evaluates exprs in a context in which *out* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [out-s# (new java.io.StringWriter)
         err-s# (new java.io.StringWriter)]
     (binding [*out* out-s#
               *err* err-s#]
       (let [result# (do ~@body)]
         {:stdout (str out-s#)
          :stderr (str err-s#)
          :result result#}))))

;(result-with-out-str
  ;(println "shit storm")
  ;(/ 1 2))



(defn handle-abortion!
  [{:as evaluation :keys [t0 result-chan long-running? new-form-evals t0]}
   {:as block :keys [id]}]
  (log/info "XXXXXXXXXX calling handle-abortion! for block" id)
  (async/>!! result-chan {:id id
                          :aborted true}))


(defn- evaluate-block!
  [{:as evaluation :keys [code-data result-chans kill-chan]}
   {:as block :keys [id code-str code-data dependencies ns-sym declares-ns]}]
  (let [block-evaluation (init-block-evaluation evaluation)
        dependency-chans (map result-chans dependencies)]
    ;; checks if a (ns ...) form, and fetches references if so
    (if-let [reference-forms (and declares-ns (ns-form-references code-data))]
      ;; We process namespace declarations synchronously, since we always want to evaluate them before the
      ;; code
      (let [t0 (System/currentTimeMillis)
            result (result-with-out-str
                     ;(with-bindings) ; should we be using with-bindings here?
                     (binding [*ns* (create-ns declares-ns)]
                       (eval (concat '(do) reference-forms))))]
        (>!! (:result-chan block-evaluation)
             (merge result {:id id :compute-time (/ (int (- (System/currentTimeMillis) t0)) 1000.)})))
      ;; Otherwise, we queue up a thread and move on
      (async/thread
        (let [[dependency-results chan] (async/alts!! [kill-chan (complete-results dependency-chans)])
              block-evaluation (assoc block-evaluation :t0 (System/currentTimeMillis))]
          (log/info "dependency-results" dependency-results)
          (when-not (= chan kill-chan)
            (if (log/spy :info (or (some :aborted dependency-results)
                                   (some :error dependency-results)))
              (handle-abortion! block-evaluation block)
              (try
                (log-long-running-form! block-evaluation block)
                (let [result
                      (result-with-out-str
                        (binding [*ns* (create-ns ns-sym)]
                          (eval code-data)))]
                  (handle-result! block-evaluation block result))
                (catch Throwable t
                  (log/error "hit a throwable")
                  (handle-error! block-evaluation block t))))))))
    ;; Note that by the beauty of immutability here, we are avoiding merging in evaluation context specific to
    ;; each block here, and only keeping the result chan from the block-evaluation in :result-chans
    (update evaluation :result-chans assoc id (:result-chan block-evaluation))))


;(let [c1 (async/chan 1)
      ;c2 (async/chan 1)]
  ;(>!! c1 :foo)
  ;(>!! c2 :bar)
  ;(Thread/sleep 100)
  ;(async/poll!
    ;;c1))
    ;(async/map vector [c1 c2] 1)))


;(let [c1 (async/to-chan (range 10))
      ;c2 (async/to-chan (range -10 0))
      ;c3 (all-complete? [c1 c2])]
  ;(Thread/sleep 100)
  ;(async/poll! c3))
  ;(async/poll!
    ;(async/map + [c1 c2] 1)))

;(let [c (async/promise-chan)]
  ;(>!! c :done)
  ;(<!! c)
  ;(<!! c)
  ;(<!! c))

(defn previous-result
  [{:keys [result-chans]}
   block-id]
  (get result-chans block-id))

;(let [c (async/thread (Thread/sleep 1000) :done)]
  ;(<!! c)
  ;(<!! c)
  ;(<!! c))

(defn apply-previous-result
  [evaluation block-id result-chan]
  (assoc-in evaluation [:result-chans block-id] result-chan))

(defn build-evaluation [analysis]
  (merge analysis
    {:kill-chan (async/promise-chan)
     :complete-chan (async/promise-chan)
     :result-chans {}}))


(defn evaluate-blocks!
  [previous-evaluation
   {:as analysis :keys [ns-sym block-id-seq blocks-by-id]}]
  (->> block-id-seq
    (reduce
      (fn [evaluation block-id]
        (let [{:as block :keys [type]}
              (blocks-by-id block-id)]
          (if (#{:code :hiccup} type)
            (if-let [result-chan (previous-result previous-evaluation block-id)]
              (apply-previous-result evaluation block-id result-chan)
              (evaluate-block! evaluation block))
            evaluation)))
      (build-evaluation analysis))))


;; Used this test code to write evaluations, but seems to not run now
(comment
  (def test-evaluation
    (let [code-str "(ns hip.hop) (def stuff :blah)\n(str stuff \"dude\")"
          analysis-results (analysis code-str)
          evaluation (evaluate-blocks! {} analysis-results)]
      (Thread/sleep 500)
      evaluation))
  (->> (:result-chans test-evaluation)
       (map #(vector (first %) (-> % second async/poll!))))
  (async/poll!
    (second (first (:result-chans test-evaluation))))
  :end-comment)


(defn lazy? [x]
  (instance? clojure.lang.LazySeq x))

;(lazy? (map inc (range 3)))

(defn result-chan-seq
  [{:keys [block-id-seq result-chans]}]
  (->> block-id-seq
       (map result-chans)
       (filter identity)))


(defn queue-result-callback!
  [{:keys [block-id-seq result-chans]}
   callback-fn]
  (doseq [block-id block-id-seq
          :let [result-chan (get result-chans block-id)]
          :when result-chan]
    (go
      (let [result (<! result-chan)]
        (callback-fn result)))))

;(queue-result-callback!
  ;test-evaluation
  ;(fn [result]
    ;;(println "have result for block" block)
    ;(println "result" result)))

;; How do errors propogate?


(defn eval-complete?
  [evaluation]
  (all-complete? (result-chan-seq evaluation)))


(defn queue-completion-callback!
  [{:as evaluation :keys [kill-chan block-id-seq blocks-by-id result-chans]}
   callback-fn]
  (async/thread
    (let [[_ chan] (async/alts!! [kill-chan (eval-complete? evaluation)])]
      (when-not (= chan kill-chan)
        (callback-fn evaluation)))))

(let [c (async/chan 1)]
  (try
    (/ 1 0)
    (catch Exception e
      (>!! c e)))
  (async/poll! c))

;stuff

;; :evaluations -> list so that conj adds to head, and can take n-1 to clear history

(defonce evaluation-state
  (atom {:evaluation-sets '()
         :kill-chan (async/promise-chan)}))

(defn last-evaluation-set
  [{:as evaluation-state-value :keys [evaluation-sets]}]
  (first evaluation-sets))

(defn shift-evaluation-set
  "Replaces the "
  [{:as evaluation-state-value :keys [history-depth]}
   new-evaluation-set]
  (update evaluation-state-value
          :evaluation-sets
          #(conj (if (>= (count %) history-depth)
                   (butlast %)
                   %)
                 new-evaluation-set)))

(defn kill-evaluation! [{:keys [kill-chan]}]
  (when kill-chan
    (go (>! kill-chan :kill))))


;; evaluation-state
;;   {:evaluation-sets
;;     ({:primary-evaluations
;;       :dependency-evaluations
;;       :dependent-evaluations})}
;;       


(defn initialize-build!
  [build-specs config]
  (->> build-specs
       (reduce
         (fn [{:as build}
              build-spec]))))


(defn reload-file!
  [file]
  (let [filename (live/canonical-path file)
        contents (slurp file)
        {:keys [last-contents last-evaluation]} (get-in @evaluation-state [:filename])]
    ;; kill last evaluation
    (kill-evaluation! last-evaluation)
    ;; Start the evaluation
    (let [evaluation (-> contents analysis (assoc :file file) (->> (evaluate-blocks! last-evaluation)))]
      ;; cache evaluation object in build state
      (log/info (utils/color-str utils/ANSI_GREEN "Reloading file: " filename))
      (swap! evaluation-state assoc filename {:last-contents contents :last-evaluation evaluation :previous-evaluation last-evaluation})
      evaluation)))
      ;(log/info (utils/color-str utils/ANSI_GREEN "Done reloading file: " filename "\n")
      ;(catch Exception _
        ;(log/error (utils/color-str utils/ANSI_RED "Unable to process all of file: " filename "\n"))
        ;; Update forms in our state atom, only counting those forms which successfully ran
          ;(compile-forms-hiccup form-evals)

;; TODO need to be able to attach metadata that says that there's a random process, meaning that new runs will
;; have to be mixed with some random key
;; Actually... this should probably just be a `^:oz/force-rerun!` key

(defn changed-block-result?
  "Not entirely sure there won't be race conditions with this"
  [{:keys [id]}]
  (let [{:keys [results-by-id]}
        (get-in @evaluation-state [:filename :previous-evaluation])]
    (get results-by-id id)))
       
(defn queue-new-result-callback!
  [previous-evaluation
   {:keys [block-id-seq result-chans]}
   callback-fn]
  (doseq [block-id block-id-seq
          :let [result-chan (get result-chans block-id)]
          :when result-chan]
    (if-let [result-chan (get-in previous-evaluation [:result-chans block-id])]
      (go
        (log/info "result chan" result-chan)
        (let [result (<! result-chan)]
          (callback-fn result))))))


(defn process-update-batch!
  [build-specs update-batch]
  (let [updates (compress-updates update-batch)]
    ()))



;;; view! should be like build, but only produce views!

;(def ^:private default-config
  ;{:live? true
   ;:view? true
   ;:lazy? true
   ;:port 5760
   ;:host "localhost"})

;(defn- infer-root-dir
  ;[build-descs]
  ;;; not correct; mocking
  ;(->> build-descs
       ;(map (comp live/canonical-path :to))
       ;(reduce live/greatest-common-path)))

;(def ^:private default-build-desc
  ;{:out-path-fn utils/html-extension})


(defn kill-build! []
  (kill-watcher! @current-watcher)
  ;; TODO make sure to plumb kill-chan through
  (kill-evaluation! (last-evaluation-set @evaluation-state)))



;(let [code-str "(def stuff :blah)\n(str stuff \"dude\")"]
   ;(-> code-str parse-code process-blocks infer-dependencies))

;(map :dependencies (infer-dependencies (process-blocks (parse-code (slurp "test.clj")))))

;(doseq [{:keys [code type defined-vars]} (process-blocks (parse-code (slurp "test.clj")))]
  ;(when (#{:code :hiccup} type)
    ;;(clojure.pprint/pprint block)
    ;(println code)
    ;(println "=> " defined-vars "\n")))

;(first
;(analyze-block
  ;(nth
    ;(parse-code (slurp "test.clj"))
    ;2))


;(->
  ;(parse-code (slurp "test.clj"))
  ;(first)
  ;:forms
  ;(without-whitespace))


;(hasch/uuid {:some 'random-data})

;(hasch/b64-hash
  ;{:some 'random-data})


;(defn- process-code
  ;[{:as block :keys [code]}])

;(->> parsed-code





;; analysis

;(require '[clojure.tools.analyzer.jvm :as ana.jvm])
;(ana.jvm/analyze '(defmulti shit first))
;(macroexpand '(defmulti shit first))
;(macroexpand '(defmethod shit :stuff [_ crap] (println crap)))
;(ana.jvm/analyze '(def shit "yo"))
;(ana.jvm/analyze '(str shit "yo"))
;(ana.jvm/analyze '(do (clojure.string/split "this,that" #",") (Thread/sleep 1000) (+ 3 4)))

;(ana.jvm/analyze '(+ 3 4))
;(ana.jvm/analyze '(def shit "yo"))
;(ana.jvm/analyze '(str shit "dawg"))
;(ana.jvm/analyze '(do (def willit? "yay!") (println willit?)))
;(ana.jvm/analyze '(defn fun [] "yay!"))

;(ana/analyze (reader/read-string (slurp "test.clj")) {})
;(ana/analyze '(+ 3 4) {})

;; gneiss...



;; goals
;;
;; * [x] opt to include code in output
;;   * [ ] opt between marginalia and inline mode
;; * [x] differential transmission of results

;; sketch for how to solve problem:
;; * [x] organize data into blocks
;;   * [x] blocks are either :md-comments, or :code
;; * [x] assign each block a hasch
;; * [x] build dependency tree between hasches
;; * [ ] run diff on sequences of block hashes
;; questions
;;   * versioning between different blocks as relates to update decisions?
;;   * how does this relate to scons and file targets?
;;     * special form for identifying a file target?
;;     * versioninng of files?


;; TODO How do we inject content into our markdown blocks?

;; Something like this? @{{(ds/row-count data)}}

;; Something like this? @{(ds/row-count data)}@

;; Something like this? @{(ds/row-count data)}

;; Something like this? {{(ds/row-count data)}}


;; TODO How do we inject content into our markdown blocks?

;; TODO How do we namespace markdown files?
;; * I guess just use a ns declaration or assume user?



(defrecord PaginatedCollection
  [id cursor page])

;(->PaginatedCollection. 3 4 5)
;(map->PaginatedCollection
  ;{:id 3 :cursor 4 :page 5})

(defn create-paginated-collection
  [props]
  (map->PaginatedCollection props))

;(create-paginated-collection {:id 7})

;(with-bindings [#'clojure.core/*data-readers*
                ;(assoc *data-readers* 'oz.next/PaginatedCollection #'oz.next/create-paginated-collection)]
  ;*data-readers*)
;(map #(vector (type (first %)) (type (second %)))
     ;*data-readers*)
;#'*data-readers*

;#oz.next/PaginatedCollection {:id 3 :cursor {:x :y} :page 10}

;(defn result-zipper
  ;(zip/zipper
    ;(fn [n] (map? n))
    ;(fn [n] (->> (dissoc n :env)
                 ;(filter (fn [[k v]]
                           ;(and (not (#{:env :form :arglists :raw-forms} k))
                                ;(coll? v))))
                 ;(mapcat (fn [[_ v]]
                           ;(if (sequential? v) v [v])))
                 ;(filter coll?)
                 ;(remove nil?)))
    ;(fn [n _] n)
    ;analysis))




