#!/usr/bin/env bb

(require '[clojure.data.xml :as xml]
         '[clojure.string :as string])


(def clojupyter-version
  (-> (slurp "deps.edn")
      (read-string)
      :aliases
      :clojupyter
      :extra-deps
      (get 'clojupyter)
      :mvn/version))

(def clojupyter-dep
  (xml/sexp-as-element
    [:dependency
     [:groupId "clojupyter"]
     [:artifactId "clojupyter"]
     [:version clojupyter-version]
     [:scope "provided"]]))

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

(defn add-clojupyter-dep
  [{:as xml :keys [content]}]
  (assoc xml
         :content
         (map
           (fn [{:as elmt :keys [tag content]}]
             (if (and tag (= "dependencies" (name tag)))
               (update elmt :content concat [clojupyter-dep])
               elmt))
           content)))

(->> (xml/parse (io/reader "pom.xml"))
     (add-clojupyter-dep)
     (stringify-xml)
     (spit "pom.xml"))



