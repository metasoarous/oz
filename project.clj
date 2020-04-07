(defproject metasoarous/oz "1.6.0-alpha6"
  :description "Great and powerful data visualizations and scientific documents in Clojure using Vega and Vega-lite"
  :deploy-repositories {"releases" :clojars
                        "snapshots" :clojars}
  :url "http://github.com/metasoarous/oz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;; trying to get es6 to work for running vega/vega-lite
  :jvm-opts ["-Dnashorn.args=--language=es6"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439" :scope "provided"]
                 ;; this appears to be necessary for fiwheel to work for some applications
                 [org.clojure/tools.reader "1.3.2"]
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
                 ;[hiccup "1.0.5"]
                 [hiccup "2.0.0-alpha2"]
                 [com.cognitect/transit-clj  "0.8.313"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [reagent "0.8.1"]
                 [cljsjs/vega "5.9.0-0"]
                 [cljsjs/vega-lite "4.0.2-0"]
                 [cljsjs/vega-embed "6.0.0-0"]
                 [cljsjs/vega-tooltip "0.20.0-0"]
                 [markdown-clj "1.10.0"]
                 [hickory "0.7.1"]
                 [markdown-to-hiccup "0.6.2"]
                 [org.clojars.didiercrunch/clojupyter "0.1.5"]
                 ;; must be above yaml for jvm compilation fix
                 [org.flatland/ordered "1.5.7"]
                 [io.forward/yaml "1.0.9"]
                 [commonmark-hiccup "0.1.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [irresponsible/tentacles "0.6.3"]
                 [respeced "0.0.1"]
                 [org.clojure/test.check "0.10.0"]
                 ;; hot reloading experiments
                 [hawk "0.2.11"]]
                 ;[timofreiberg/bultitude "0.3.0"]
                 ;[org.clojure/tools.namespace "0.2.11"]]
  :plugins [[lein-cljsbuild "1.1.6"]]
  :source-paths ["src/clj" "src/cljs"]
  ;; allows cljdoc to fetch README and such for additional documentation purposes
  :scm {:name "git" :url "https://github.com/metasoarous/oz"}
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/oz/public/js"]
  :repl-options {:init-ns user
                 :timeout 520000}
  ;:prep-tasks ["compile" ["cljsbuild" "once" "min"]]
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

  ;; Note; for the aliases below to work, you need uberjar not to delete the cljsbuild output, which it does automatically via lein clean.
  ;; This means though that you should always uberjar with one of the below methods, to make sure you don't get some bad aot in your build/deploy.
  ;; Lack of awareness of this setting is I think what was behind the old "always build cljs" prep-tasks setting from vizard.
  :auto-clean false 
  :aliases {
            ;"doitfools" ["do" "clean" ["deploy" "clojars"]]
            "jar!"
            ^{:doc "Recompile sources and jar."}
            ;; Nested vectors are supported for the "do" task
            ["do" "clean"
                  ["cljsbuild" "once" "min"]
                  ["jar"]]
            "deploy-snapshot!"
            ^{:doc "Recompile sources, then deploy snapshot."}
            ;; Nested vectors are supported for the "do" task
            ["do" "clean"
                  ["cljsbuild" "once" "min"]
                  ["jar"]
                  ["deploy" "clojars"]]
            "deploy-release!"
            ^{:doc "Recompile sources, then deploy release."}
            ;; Nested vectors are supported for the "do" task
            ["do" "clean"
                  ["cljsbuild" "once" "min"]
                  ["vcs" "tag"]
                  ["jar"]
                  ["deploy" "clojars"]]}
             
  :profiles {:dev
             {:dependencies [[alembic "0.3.2"]
                             [binaryage/devtools "0.9.10"]
                             [figwheel-sidecar "0.5.18"]
                             [com.cemerick/piggieback "0.2.2"]]
              :plugins [[lein-figwheel "0.5.18"]]
              :source-paths ["dev"]}}
              ;:repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :main ^:skip-aot oz.server)

