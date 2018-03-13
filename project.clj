(defproject metasoarous/oz "1.3.1"
  :description "Great and powerful data visualizations in Clojure using Vega and Vega-lite"
  :url "http://github.com/metasoarous/oz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;; default release steps
  ;:release-tasks [["vcs" "assert-committed"]
                  ;["change" "version"
                   ;"leiningen.release/bump-version" "release"]
                  ;["vcs" "commit"]
                  ;["vcs" "tag"]
                  ;["deploy"]
                  ;["change" "version" "leiningen.release/bump-version"]
                  ;["vcs" "commit"]
                  ;["vcs" "push"]]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.64" :scope "provided"]
                 [org.clojure/core.async "0.4.474"]
                 [cheshire "5.8.0"]
                 [clj-http "3.7.0"]
                 [com.taoensso/sente "1.12.0"]
                 [aleph "0.4.4"]
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [bk/ring-gzip "0.2.1"]
                 [ring-cljsjs "0.1.0"]
                 [compojure "1.6.0"]
                 [com.cognitect/transit-clj  "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [reagent "0.7.0"]
                 [cljsjs/vega "3.2.1-0"]
                 [cljsjs/vega-lite "2.2.0-0"]
                 [cljsjs/vega-embed "3.1.1-0"]
                 [cljsjs/vega-tooltip "0.6.1-0"]
                 [irresponsible/tentacles "0.6.1"]]
  :plugins [[lein-cljsbuild "1.1.6"]]
  :source-paths ["src/clj" "src/cljs"]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :aliases {"doitfools" ["do" "clean" ["deploy" "clojars"]]}
  :repl-options {:init-ns user
                 :timeout 120000}
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
             {:dependencies [[binaryage/devtools "0.9.9"]
                             [figwheel-sidecar "0.5.15"]
                             [com.cemerick/piggieback "0.2.2"]]
              :plugins [[lein-figwheel "0.5.11"]]
              :source-paths ["dev"]
              :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :uberjar
             {:source-paths ^:replace ["src/clj"]
              :omit-source true
              :aot :all}}
  :main ^:skip-aot oz.server)

