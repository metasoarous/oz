(ns oz.notebook.clojupyter
  "Experimental support for rendering vega in clojupyter"
  (:require
    [oz.core :as oz]
    [lazy-require.core :as lreq]))


;; Something fancy that remembers if its been setup? Probablematic just to always run?
;(defonce -setup?
  ;(setup!))


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

;(defn v! [spec & {:keys [d]}])
  
