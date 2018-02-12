(ns user
  (:require [oz.server :refer [start! stop!]]
            [figwheel-sidecar.repl-api :as figwheel]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn run []
  (figwheel/start-figwheel!))

(defn do-it-fools! []
  (run)
  (start!))

(def browser-repl figwheel/cljs-repl)
