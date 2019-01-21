(ns oz.notebook.clojupyter
  "Experimental support for rendering vega in Jupyter with Clojupyter"
  (:require
    [oz.core :as oz]
    [lazy-require.core :as lreq]
    [hiccup.core :as hiccup]
    [clojupyter.protocol.mime-convertible :as mc]
    [clojure.data.json :as json]))



(def require-string
  "
<div id='uuid-%s'>
<script>
requirejs.config({
  baseUrl: 'https://cdn.jsdelivr.net/npm/',
  paths: {
    'vega-embed':  'vega-embed@3?noext',
    'vega-lib': 'vega-lib?noext',
    'vega-lite': 'vega-lite@2?noext',
    'vega': 'vega@3?noext'
  }
});
require(['vega-embed'], function(vegaEmbed) {
  let spec = %s;
  vegaEmbed('#uuid-%s', spec, {defaultStyle:true}).catch(console.warn);
  }, function(err) {
  console.log('Failed to load');
});
</script>
</div>
  ")


(defn- uuid [] (str (java.util.UUID/randomUUID)))

(defn- vega->html [v]
  (let [id (uuid)]
    (format require-string id (json/write-str (:spec v)) id)))



;(defn view! [spec]
  ;;; problematic to always run?
  ;(lreq/with-lazy-require
    ;[[clojupyter.protocol.mime-convertible :as mc]]
    ;(reify
      ;mc/PMimeConvertible
      ;(to-mime [this]
        ;(mc/stream-to-string
          ;{:text/html (hiccup/html (oz/embed spec {:embed-fn vega->html}))})))))

(defn view! [spec]
  ;; problematic to always run?
  (reify
    mc/PMimeConvertible
    (to-mime [this]
      (mc/stream-to-string
        {:text/html (hiccup/html (oz/embed this {:embed-fn vega->html}))}))))




;; Old notes; cruft...

;(defrecord Vega [spec]
  ;mc/PMimeConvertible
  ;(to-mime [this]
     ;(mc/stream-to-string
      ;{:text/html (vega->html this)})))

;(defn vega
  ;"Wrap a map as a Vega object"
  ;[m]
  ;(->Vega m))


;(defn view! [spec]
  ;;; problematic to always run?
  ;(lreq/with-lazy-require
    ;[[clojupyter.misc.display :as display]
     ;[clojupyter.misc.helper :as helper]]
    ;(do
      ;(helper/add-javascript "https://cdn.jsdelivr.net/npm/vega-embed@3")
      ;(display/hiccup-html 
        ;;; This should be using the embed login from core
        ;(oz/embed spec)))
;; Do we want this alias?
;(defn v! [spec & {:keys [d]}])



