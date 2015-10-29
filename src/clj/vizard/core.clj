(ns vizard.core
  (:require [vizard.server :as server]
            [org.httpkit.client :as client]
            [cheshire.core :as json]))

(def start! server/start!)

(defn plot! [spec & {:keys [host port]
                     :or {port (:port @server/web-server_ 10666)
                          host "localhost"}}]
  (client/post (str "http://" host ":" port "/spec")
               {:body (json/generate-string spec)}))
