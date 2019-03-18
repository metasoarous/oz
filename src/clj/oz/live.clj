(ns ^:no-doc oz.live
  (:require [hawk.core :as hawk]
            [taoensso.timbre :as log]
            [clojure.tools.reader.edn :as edn]))



(defonce watchers (atom {}))


(defn watch! [filename f]
  (if-not (get @watchers filename)
    (let [watcher
          (hawk/watch! [{:paths [filename]
                         :handler (fn [context event]
                                    (f filename context event))}])]
      (swap! watchers assoc-in [filename :watcher] watcher))))


(defn process-ns-form
  [ns-form]
  ;; Separate out all of the "reference forms" from the rest of the ns declaration; we'll use this in a couple
  ;; different ways later
  (let [reference-forms (->> ns-form (filter seq?) (map #(vector (first %) (rest %))) (into {}))]
    ;; the namespace sym should always be the second item in the form
    {:ns-sym
     (second ns-form)
     :reference-forms
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
         (dissoc reference-forms :refer-clojure)))}))


(defn reload-file! [filename context {:keys [kind file]}]
  ;; ignore delete (some editors technically delete the file on every save!
  (when (#{:modify :create} kind)
    (let [contents (slurp filename)
          forms (edn/read-string (str "[" contents "]"))
          last-forms (get-in @watchers [filename :last-forms])
          diff-forms (->> (map vector
                               (drop 1 forms)
                               (concat (drop 1 last-forms)
                                       (repeat nil)))
                          (drop-while (fn [[f1 f2]] (= f1 f2)))
                          (map first))
          {:keys [ns-sym reference-forms]} (process-ns-form (first forms))]
      ;; if there are differences, then do the thing
      (when (seq diff-forms)
        (log/info "Reloading file:" filename)
        ;; Evaluate the ns form, and whatever forms thereafter differ from the last time we succesfully ran
        (binding [*ns* (create-ns ns-sym)]
          (eval
            (concat '(do)
                     reference-forms
                     diff-forms)))
        ;; Update last-forms in our state atom
        (swap! watchers assoc-in [filename :last-forms] forms)))))


(defn live-reload!
  "Watch a clj file for changes, and re-evaluate only those lines which have changed, together with all following lines.
  Is not sensitive to whitespace changes, and will also always rerun reference forms included in the ns declaration."
  [filename]
  (log/info "Starting live reload on file:" filename)
  (watch! filename reload-file!))


(defn kill-watcher!
  "Kill the corresponding watcher thread."
  [filename]
  (hawk/stop! (get-in @watchers [filename :watcher]))
  (swap! watchers dissoc filename))


(defn kill-watchers!
  "Kill all watcher threads if no args passed, or just watcher threads for specified filenames if specified."
  ([] (kill-watchers! (keys @watchers)))
  ([filenames]
   (doseq [filename filenames]
     (kill-watcher! filename))))


(comment
  (live-reload! "dev/watchtest.clj")
  :end-comment)

