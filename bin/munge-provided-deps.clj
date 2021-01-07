#!/usr/bin/env bb

(require '[clojure.data.xml :as xml]
         '[clojure.string :as string]
         '[clojure.walk :as walk]
         '[clojure.pprint :as pp])

(def pom-xml
  (xml/parse (io/reader "pom.xml")))

(def pom-ns (namespace (:tag (second (:content pom-xml)))))

(defn nsify
  [key]
  (keyword pom-ns (name key)))

(def clojupyter-version
  (-> (slurp "deps.edn")
      (read-string)
      :aliases
      :clojupyter
      :extra-deps
      (get 'clojupyter/clojupyter)
      :mvn/version))

(println "clojupyter version is:" clojupyter-version)

(def clojupyter-dep
  (xml/sexp-as-element
    [(nsify :dependency)
     [(nsify :groupId) "clojupyter"]
     [(nsify :artifactId) "clojupyter"]
     [(nsify ::version) clojupyter-version]
     [(nsify ::scope) "provided"]]))

(def strip-emtpy-lines
  (partial remove
           (comp empty? string/trim)))


(defn stringify-xml [xml]
  (->> xml
    (xml/indent-str)
    (string/split-lines)
    (strip-emtpy-lines)
    (interpose "\n")
    (apply str)))

(defn decorate-xml
  [xml attrs]
  (walk/postwalk
    (fn [{:as elmt :keys [tag]}]
      (if tag
        (update elmt :attrs merge attrs)
        elmt))
    xml))

(defn add-clojupyter-dep
  [{:as xml :keys [content attrs]}]
  (let [clojupyter-dep clojupyter-dep]
  ;(let [clojupyter-dep (decorate-xml clojupyter-dep attrs)]
    (assoc xml
           :content
           (map
             (fn [{:as elmt :keys [tag content]}]
               (if (and tag (= "dependencies" (name tag)))
                 (update elmt :content concat [clojupyter-dep])
                 elmt))
             content))))

(->> pom-xml
     (add-clojupyter-dep)
     (xml/emit-str)
     ;(println))
     (spit "pom.xml"))



