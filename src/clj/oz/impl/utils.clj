(ns oz.impl.utils
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(defn apply-opts
  "utility function for applying kw-args"
  [f & args]
  (apply f (concat (butlast args) (flatten (into [] (last args))))))

(defn html-extension
  "given a path, returns the relative path as an html filename"
  [relative-path]
  (string/replace relative-path #"\.\w*$" ".html"))

(defn- drop-extension
  [relative-path]
  (string/replace relative-path #"\.\w*$" ""))

(defn canonical-path
  [path-or-file]
  (-> (io/file path-or-file)
      (.getCanonicalFile)
      (.getPath)))

(defn relative-path
  [path base]
  (string/replace (canonical-path path)
                  (canonical-path base)
                  ""))

;; Some path helpers

(defn strip-path-seps
  [path]
  (if (= (last path)
         (java.io.File/separatorChar))
    (strip-path-seps (apply str (drop-last path)))
    path))

(defn split-path
  [path]
  (string/split path #"\/"))

(defn greatest-common-path
  [path1 path2]
  (->>
    (map vector (split-path path1) (split-path path2))
    (filter (partial apply =))
    (map first)
    (string/join (java.io.File/separatorChar))))

(defn join-paths
  [path1 path2]
  (str (strip-path-seps path1)
       (java.io.File/separatorChar)
       path2))

(defn ext
  [file]
  (-> (.getPath (io/file file))
      (string/split #"\.")
      (last)))

(defn compute-out-path
  [{:as build-desc :keys [from to out-path-fn]} path]
  (let [out-path-fn (or out-path-fn drop-extension)
        single-file? (= (str (.getAbsolutePath (io/file path)))
                        (str (.getAbsolutePath (io/file from))))
        to-dir? (or (.isDirectory (io/file to))
                    (= (last (.getPath (io/file path))) (java.io.File/separatorChar)))
        relative-from-path (if single-file? path (relative-path path from))]
    (if (and single-file? (not to-dir?))
      ;; then we're just translating a single file with an explicit to path
      to
      ;; then we need to assume that we're exporting to a path which has a directory created for it
      (join-paths (or to ".")
                  (out-path-fn relative-from-path)))))


(def ANSI_RED "\u001B[31m")
(def ANSI_GREEN "\u001B[32m")
(def ANSI_YELLOW "\u001B[33m")
(def ANSI_BLUE "\u001B[34m")
(def ANSI_RESET "\u001B[0m")

(defn color-str
  [color & ss]
  (str color
       (apply str ss)
       ANSI_RESET))


