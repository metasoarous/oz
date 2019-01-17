(ns oz.notebook.iclojure
  "Experimental support for rendering vega in clojupyter"
  (:require
    [oz.core :as oz]
    [lazy-require.core :as lreq]
    [hiccup.core :as hiccup]
    [cheshire.core :as json]))


;; Something fancy that remembers if its been setup? Probablematic just to always run?
;(defonce -setup?
  ;(setup!))


(defn view! [spec]
  ;; problematic to always run?
  (tagged-literal
    'unrepl/mime
    (if (map? spec)
      {:content-type "application/vnd.vegalite.v2+json"
       :content spec}
      ;; otherwise assume hiccup, run embed code
      {:content-type "text/html"
       :content (hiccup/html (oz/embed spec))})))

