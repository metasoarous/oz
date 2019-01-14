(ns oz.notebook.clojupyter
  "Experimental support for rendering vega in Jupyter with Clojupyter"
  (:require
    [oz.core :as oz]
    [lazy-require.core :as lreq]))


(defn view! [spec]
  ;; problematic to always run?
  (lreq/with-lazy-require
    [[clojupyter.misc.display :as display]
     [clojupyter.misc.helper :as helper]]
    (do
      (helper/add-javascript "https://cdn.jsdelivr.net/npm/vega-embed@3")
      (display/hiccup-html 
        ;; This should be using the embed login from core
        (oz/embed spec)))))

;; Do we want this alias?
;(defn v! [spec & {:keys [d]}])
  
