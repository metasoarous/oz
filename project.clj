(defproject metasoarous/oz "1.5.4"
  :description "Great and powerful data visualizations in Clojure using Vega and Vega-lite"
  :deploy-repositories {"releases" :clojars
                        "snapshots" :clojars}
  :url "http://github.com/metasoarous/oz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;; trying to get es6 to work for running vega/vega-lite
  :jvm-opts ["-Dnashorn.args=--language=es6"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439" :scope "provided"]
                 [org.clojure/core.async "0.4.490"]
                 [cheshire "5.8.1"]
                 [clj-http "3.9.1"]
                 [com.taoensso/sente "1.13.1"]
                 [com.clojure-goes-fast/lazy-require "0.1.1"]
                 [aleph "0.4.6"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [bk/ring-gzip "0.3.0"]
                 [ring-cljsjs "0.1.0"]
                 [compojure "1.6.1"]
                 [hiccup "1.0.5"]
                 [com.cognitect/transit-clj  "0.8.313"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [reagent "0.8.1"]
                 [cljsjs/vega "4.4.0-0"]
                 [cljsjs/vega-lite "3.0.0-rc10-0"]
                 [cljsjs/vega-embed "3.26.0-0"]
                 [cljsjs/vega-tooltip "0.14.0-0"]
                 [markdown-to-hiccup "0.6.1"]
                 [org.clojars.didiercrunch/clojupyter "0.1.5"]
                 [io.forward/yaml "1.0.9"]
                 [commonmark-hiccup "0.1.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [irresponsible/tentacles "0.6.3"]]
  :plugins [[lein-cljsbuild "1.1.6"]]
  :source-paths ["src/clj" "src/cljs"]
  ;; allows cljdoc to fetch README and such for additional documentation purposes
  :scm {:name "git" :url "https://github.com/metasoarous/oz"}
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/oz/public/js"]
  :aliases {"doitfools" ["do" "clean" ["deploy" "clojars"]]}
  :repl-options {:init-ns user
                 :timeout 520000}
  :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "oz.app/on-js-reload"}
                        :compiler {:main oz.app
                                   :asset-path "js/compiled/out"
                                   :output-to "resources/oz/public/js/compiled/oz.js"
                                   :output-dir "resources/oz/public/js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [devtools.preload]}}
                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/oz/public/js/compiled/oz.js"
                                   :main oz.app
                                   :optimizations :advanced
                                   :pretty-print false}}]}
  :figwheel {
             ;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/oz/public/css"]} ;; watch and update CSS

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
             {:dependencies [[binaryage/devtools "0.9.10"]
                             [figwheel-sidecar "0.5.18"]
                             [com.cemerick/piggieback "0.2.2"]]
              :plugins [[lein-figwheel "0.5.11"]]
              :source-paths ["dev"]}
              ;:repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :uberjar
             {:source-paths ^:replace ["src/clj"]
              :omit-source true
              :aot :all}}
  :main ^:skip-aot oz.server)

