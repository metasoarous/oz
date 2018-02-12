(defproject yieldbot/oz "1.0.1"
  :description "Magic Visualization"
  :url "http://github.com/metasoarous/oz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.854" :scope "provided"]
                 [org.clojure/core.async "0.2.374"]
                 [cheshire "5.7.0"]
                 [clj-http "2.2.0"]
                 [com.taoensso/sente "1.11.0"]
                 [aleph "0.4.1"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.3"]
                 [bk/ring-gzip "0.1.1"]
                 [ring-cljsjs "0.1.0"]
                 [compojure "1.5.0"]
                 [com.cognitect/transit-clj  "0.8.297"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [reagent "0.6.1"]
                 [cljsjs/vega "3.0.7-0"]
                 [cljsjs/vega-lite "2.0.0-0"]
                 [cljsjs/vega-embed "3.0.0-rc7-0"]
                 [cljsjs/vega-tooltip "0.4.4-0"]
                 [com.rpl/specter "0.9.1"]]
  :plugins [[lein-cljsbuild "1.1.6"]]
  :source-paths ["src/clj" "src/cljs"]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :aliases {"doitfools" ["do" "clean" ["deploy" "clojars"]]}
  :repl-options {:init-ns user}
  :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "oz.core/on-js-reload"}
                        :compiler {:main oz.core
                                   :asset-path "js/compiled/out"
                                   :output-to "resources/public/js/compiled/oz.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [devtools.preload]}}
                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/oz.js"
                                   :main oz.core
                                   :optimizations :advanced
                                   :pretty-print false}}]}
  :figwheel {
             ;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"]} ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             
  :profiles {:dev
             {:dependencies [[binaryage/devtools "0.9.4"]
                             [figwheel-sidecar "0.5.11"]
                             [com.cemerick/piggieback "0.2.2"]]
              :plugins [[lein-figwheel "0.5.11"]]
              :source-paths ["dev"]
              :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :uberjar
             {:source-paths ^:replace ["src/clj"]
              :omit-source true
              :aot :all}}
  :main ^:skip-aot oz.server)
