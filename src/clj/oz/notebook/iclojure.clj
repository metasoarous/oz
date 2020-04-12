(ns oz.notebook.iclojure
  "Experimental support for rendering vega in Jupyter with IClojure"
  (:require
    [oz.core :as oz]
    ;[lazy-require.core :as lreq]
    [hiccup.core :as hiccup]
    [cheshire.core :as json]))


(defn view!
  "Display a vega or vega-lite spec from a Jupyter notebook using the IClojure Jupyter kernel."
  [spec]
  (tagged-literal
    'unrepl/mime
    (if (map? spec)
      ;; Don't know if this will work for vega yet
      {:content-type "application/vnd.vegalite.v2+json"
       :content spec}
      ;; otherwise assume hiccup, run embed code
      {:content-type "text/html"
       :content (hiccup/html (oz/embed spec))})))

