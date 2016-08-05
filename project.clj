(defproject yieldbot/vizard "0.1.8"
  :description "Magic Visualization"
  :url "http://github.com/yieldbot/vizard"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [org.clojure/core.async "0.2.374"]
                 [cheshire "5.5.0"]
                 [clj-http "2.2.0"]
                 [com.taoensso/sente "1.9.0"]
                 [com.taoensso/timbre "4.5.1"]
                 [com.taoensso/encore "2.68.1"]
                 [aleph "0.4.1"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [compojure "1.4.0"]
                 [com.cognitect/transit-clj  "0.8.285"]
                 [sablono "0.3.6"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.3.12"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [cljsjs/vega "2.6.0-0"]
                 [cljsjs/vega-lite "1.1.1-0"]
                 [com.rpl/specter "0.9.1"]]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.2"]]
  :source-paths ["src/clj"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
  :aliases {"doitfools" ["do" "clean" ["deploy" "clojars"]]}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "vizard.core/on-js-reload"}
                        :compiler {:main vizard.core
                                   :asset-path "js/compiled/out"
                                   :output-to "resources/public/js/compiled/vizard.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true}}
                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/vizard.js"
                                   :main vizard.core
                                   :optimizations :advanced
                                   :pretty-print false}
                        :jar true}]}
  :figwheel {
             ;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

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
             }
  :profiles {:uberjar {:aot :all}}
  :main ^:skip-aot vizard.server)
