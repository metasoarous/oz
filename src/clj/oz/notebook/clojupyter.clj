(ns oz.notebook.clojupyter
  "Experimental support for rendering vega in Jupyter with Clojupyter"
  (:require
    [oz.core :as oz]
    ;[lazy-require.core :as lreq]
    [taoensso.timbre :as log]
    [hiccup.core :as hiccup]
    ;[clojupyter.protocol.mime-convertible :as mc]
    [clojure.data.json :as json]))


(try
  (require '[clojupyter.protocol.mime-convertible :as mc])
  (catch Throwable _
    (log/debug "Unable to require clojupyter; Please explicitly add clojupyter to your dependencies if you would like to use it.")))


(try
  (require '[clojupyter.protocol.mime-convertible :as mc])

  (def ^:no-doc require-string
    (str "
  <div>
    <div id='uuid-%s'></div>
    <script>
    requirejs.config({
      baseUrl: 'https://cdn.jsdelivr.net/npm/',
      paths: {
        'vega-embed':  'vega-embed@" oz/vega-embed-version "?noext',
        'vega-lib': 'vega-lib?noext',
        'vega-lite': 'vega-lite@" oz/vega-lite-version "?noext',
        'vega': 'vega@" oz/vega-version "?noext'
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
    "))


  (defn- uuid [] (str (java.util.UUID/randomUUID)))

  (defn- live-embed [v]
    (let [id (uuid)]
      (format require-string id (json/write-str v) id)))


  ;; Would ideally like to use lazy require so that clojupyter isn't a hard requirement, but unfortunately, it doesn't work with reify presently
  ;(defn view! [spec]
    ;;; problematic to always run?
    ;(lreq/with-lazy-require
      ;[[clojupyter.protocol.mime-convertible :as mc]]
      ;(reify
        ;mc/PMimeConvertible
        ;(to-mime [this]
          ;(mc/stream-to-string
            ;{:text/html (hiccup/html (oz/embed spec {:embed-fn live-embed}))})))))


  ;; NOTE This function has been duplicated from oz.core in order to avoid a bug with loading in Clojupyter.
  ;; TODO Once that underlying bug has been resolved, we should remove this duplication, and refer to oz.core again.
  (defn ^:no-doc embed
    "Take hiccup or vega/lite spec and embed the vega/lite portions using vegaEmbed, as hiccup :div and :script blocks.
    When rendered, should present as live html page; Currently semi-private, may be made fully public in future."
    ([spec {:as opts :keys [embed-fn] :or {embed-fn live-embed}}]
     ;; prewalk spec, rendering special hiccup tags like :vega and :vega-lite, and potentially other composites,
     ;; rendering using the components above. Leave regular hiccup unchanged).
     ;; TODO finish writing; already hooked in below so will break now
     (if (map? spec)
       (embed-fn spec)
       (clojure.walk/prewalk
         (fn [x] (if (and (coll? x) (#{:vega :vega-lite} (first x)))
                   (embed-fn (second x))
                   x))
         spec)))
    ([spec]
     (embed spec {})))

  (defn view!
    "Display a vega or vega-lite spec from a Jupyter notebook using the Clojupyter kernel."
    [spec]
    (reify
      ;mc/PMimeConvertible
      clojupyter.protocol.mime-convertible/PMimeConvertible
      (to-mime [this]
        ;(mc/stream-to-string
        (clojupyter.protocol.mime-convertible/stream-to-string
          ;; TODO switch back to oz.core/embed once this issue is resolved
          ;{:text/html (hiccup/html (oz/embed spec {:embed-fn live-embed}))}
          {:text/html (hiccup/html (oz/embed-for-html spec))}))))
  
  (catch Exception e
    (log/debug "Unable to compile oz.notebook.clojupyter; Please explicitly add clojupyter to your dependencies if you would like to use it.")))


