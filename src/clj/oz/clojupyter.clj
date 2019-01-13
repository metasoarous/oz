(ns oz.clojupyter
  "Experimental support for rendering vega in clojupyter"
  (:require
    [lazy-require.core :as lreq]
    [clojure.data.json :as json]))


;; Something fancy that remembers if its been setup? Probablematic just to always run?
;(defonce -setup?
  ;(setup!))

(defn prep-spec
  ([spec mode]
   (clojure.walk/prewalk
     (fn [x] (if (and (coll? x) (#{:vega :vega-lite} (first x)))
               [:div [:div {:id id}
                          [:script spec]]]
               [(case (first x) :vega vega :vega-lite vega-lite)
                (reduce merge (rest x))]
               x))
     spec))
  (prep-spec spec nil))

(defn view! [spec]
  ;; problematic to always run?
  (lreq/with-lazy-require
    [[clojupyter.misc.display :as display]
     [clojupyter.misc.helper :as helper]]
    (let [id (str (java.util.UUID/randomUUID))
          code (format "vegaEmbed('#%s', %s, {'mode': 'vega-lite'});" id, (json/write-str vega-json))]
        (helper/add-javascript "https://cdn.jsdelivr.net/npm/vega-embed@3")
        (display/hiccup-html 
          [:div [:div {:id id}
                     [:script spec]]]))))

;(defn v! [spec & {:keys [d]}])
  
