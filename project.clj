(defproject yieldbot/vizard "0.1.0-SNAPSHOT"
  :description "Magic Visualization"
  :url "http://github.com/yieldbot/vizard"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cheshire "5.5.0"]
                 [com.taoensso/sente "1.7.0"]
                 [com.taoensso/timbre "4.1.2"]
                 [http-kit "2.1.19"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.4.0"]
                 [com.cognitect/transit-clj  "0.8.281"]
                 [sablono "0.3.6"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.3.12"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [cljsjs/vega "2.2.5-0"]
                 [com.rpl/specter "0.8.0"]]
  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.4.1"]]
  :source-paths ["src/clj"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
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
                                   :optimizations :whitespace
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
             })
