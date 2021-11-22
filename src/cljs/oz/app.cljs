(ns ^:no-doc ^:figwheel-always oz.app
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.core.async :as async  :refer (go go-loop <! >! chan)]
            [taoensso.encore :as encore :refer-macros (have have?)]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
            [clojure.string :as str]
            [clojure.stacktrace :as st]
            [taoensso.sente :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]
            [oz.core :as core]
            [re-highlight.core :as highlight])
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(timbre/set-level! :info)
(enable-console-print!)

(defonce app-state (r/atom {:text "Pay no attention to the man behind the curtain!"
                            :document nil
                            :async-block-results {}
                            :error nil}))

;; TODO Build in garbage collection, so that results are cleared out after they haven't been used some set
;; number of times

;(defonce async-block-results (r/cursor app-state [:async-block-results]))
;(add-watch async-block-results
           ;:async-block-results-update
           ;(fn [k r o n]
             ;(js/console.log "XXX" n)))
(def async-block-results
  (r/reaction
    (get @app-state :async-block-results)))

@async-block-results
;(:document @app-state)

(let [packer (sente-transit/get-transit-packer)
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk"
                                         {:type :auto
                                          :packer packer})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))


;; Handle top level sente wire connection details

(defmulti -sente-event-handler
  "Handles the sente messages coming off the wire, which are important for managing the lifecycle of
  the sente connection."
  :id)

(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (debugf "Event: %s" event) []
  (-sente-event-handler ev-msg))

(defmethod -sente-event-handler :default
  [{:as ev-msg :keys [event]}]
  (debugf "Unhandled event: %s" event))

(defmethod -sente-event-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] (have vector? ?data)]
    (if (:first-open? new-state-map)
      (debugf "Channel socket successfully established!: %s" ?data)
      (debugf "Channel socket state change: %s" ?data))))

(defmethod -sente-event-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (debugf "Handshake: %s" ?data)))



;; Handle domain messages coming off the wire

(defmulti message-handler
  "Handler for domain messages off the wire."
  first)

;; fallback; log out to console
(defmethod message-handler :default
  [data]
  (debugf "Unrecognized push event from server: %s" data))

;; install the message-handler
(defmethod -sente-event-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (let [[id msg] ?data]
    (message-handler ?data)))

(defn typeset-mathjax! []
  ;; Make sure (using timeouts) that if there's a delay in rendering, we're more likely
  ;; to catch it
  (async/go
    (<! (async/timeout 5))
    (.typeset js/MathJax)
    (async/go
      (<! (async/timeout 2500))
      (.typeset js/MathJax))))


;; old view-doc method
(defmethod message-handler :oz.core/view-doc
  [[_ msg]]
  (swap! app-state merge {:document msg :error nil})
  (typeset-mathjax!))

;(defmethod message-handler :oz.core/view-block
  ;[[_ {:keys [id block-eval]}]]
  ;(swap! app-state assoc-in [:blocks id] block-eval))

(defmethod message-handler :oz.core/async-block-results
  [[_ {:as block :keys [id]}]]
  (js/console.log ":oz.core/async-block-results are in for block id: " (pr-str id))
  ;(js/console.log "block:" (pr-str block))
  (swap! app-state assoc-in [:async-block-results id ] block)
  (typeset-mathjax!))

;; or for omit-styles? etc
(defmethod message-handler :oz.core/update-header-extras
  [[_ {:as block :keys [id]}]]
  (js/console.log ":oz.core/async-block-results are in for block id: " (pr-str id))
  ;(js/console.log "block:" (pr-str block))
  (swap! app-state assoc-in [:async-block-results id ] block))

;; build our custom views for code blocks and such

;; questios:
;; * [ ] how do we keep track of what blocks need to be garbage collected?
;;   * [ ] can we attach a will-unmount hook to the components?
;;   * right now we are banking on using the existing tree-traversing to make sure

;(defn get-block-result
  ;[id]
  ;(r/cursor async-block-results [id]))

(defn get-block-result
  [id]
  (r/reaction
    (get @async-block-results id)))

(defn src-view [{:keys [id code-str]}]
  [:div {:style {:margin-left -10 :margin-right -10}}
    [highlight/highlight
     {:language "clojure"}
     code-str]])

;(keys @async-block-results)

(defn status-message
  [emoji-str message]
  [:span [:span {:style {:margin-right 8}} emoji-str] message])

(defn running-status
  [async-result]
  (let [run-time (r/atom 0)]
    (go-loop []
      (<! (async/timeout 1000))
      (when-not @async-result
        (swap! run-time inc)
        (recur)))
    (fn [_]
      [status-message "⌛" (str "Running... (t = " @run-time "s)")])))

(defn eval-status [id async-result]
  [:p {:class :sans :style {:font-size 9 :text-align :right :margin-top 0}}
   (if-let [{:keys [compute-time]} @async-result]
     [status-message "✅" (str "Finished (t = " compute-time"s)")]
     [running-status async-result])])

(def small-annotation-styles
  {:font-size 9 :color "grey"})

(defn block-id-view
  [id]
  [:p {:class "sans" :style (merge small-annotation-styles {:text-align :right :margin-bottom -12})}
   "block: "
   [:code {:style small-annotation-styles} (str id)]])

(defn dependencies-view
  [{:keys [dependencies]}]
  (let [show-dependencies? (r/atom false)]
    (fn [{:keys [dependencies]}]
      @show-dependencies?
      (when (seq dependencies)
        [:div
         {:style (merge small-annotation-styles)}
         [:a
          {:style {:color "grey"}
           :class :sans
           :on-click (fn [& _]
                       (swap! show-dependencies? not))}
          [:span {:style {:font-size 8 :vertical-align :top}}
           (if @show-dependencies? "▼" "▶")]
          ;(if @show-dependencies? "⌄ " "> ")
          " Dependencies: "]
         (when @show-dependencies?
           [:ul
            {:style {:margin-top 2}}
            (for [dep dependencies]
              [:li [:a {:href (str "/#" dep)
                        :style {:color :grey
                                :text-decoration :none}}
                     (str dep)]])])]))))


(defn code-view
  [{:as block :keys [display-src? id] :or {display-src? true}}]
  (js/console.log "updating code-view component for id: " (pr-str id))
  (let [async-result (get-block-result id)]
    (when display-src?
      [:div
       {:style {:padding-top 4}}
       [block-id-view id]
       [src-view block]
       [:div {:style {:display :flex
                      :flex-flow "row nowrap"
                      :justify-content :space-between
                      :margin-top -10}}
        [:div {:style {:max-width "40%"}}; :margin :auto}}
         [dependencies-view block]]
        [:div {:style {:max-width "40%"}}; :magin :auto}}
         [eval-status id async-result]]]])))
       ;(when)])))

(defn hiccup-view
  [{:as block :keys [display-src? id] :or {display-src? true}}]
  (js/console.log "updating hiccup-view component for id: " (pr-str id))
  (let [async-result (get-block-result id)]
    [:div
      {:style {:padding-top 4}}
      (when display-src?
        [code-view block])
      (when-let [{:keys [result]} @async-result]
        [core/live-view result])]))

@(r/cursor app-state [:async-block-results #uuid "3687ff30-622b-52fb-b4da-31176341cba5"])
(get @async-block-results #uuid "3687ff30-622b-52fb-b4da-31176341cba5")


(defn error-boundary
  [component]
  (r/create-class
    {:component-did-catch (fn [this e info]
                            (swap! app-state assoc :error e))
     :reagent-render (fn [comp]
                       (if-let [error (:error @app-state)]
                         [:div
                          [:h2 "Unable to process document!"]
                          [:h3 "Error:"]
                          [:code (pr-str error)]
                          [:h4 "Please check console log for error"]]
                         comp))}))


(defn async-block-view
  [{:as block :keys [id type hiccup]}]
  [error-boundary
   [:div
    {:id id}
    (case type
      ;:md-comment hiccup
      ;:markdown hiccup
      :hiccup [hiccup-view block]
      :code [code-view block] 
      :code-comment [src-view block]
      ;; default
      [:div [:h3 "Unknown block type" type]
       [:pre (pr-str block)]])]]) 

(core/register-live-views
  :oz.doc/async-block async-block-view
  :oz.doc/code-comment src-view)


;; Set up sente router

(def router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-client-chsk-router! ch-chsk event-msg-handler)))


(defn application [app-state]
  (if-let [doc (:document @app-state)]
    [:div [core/live-view doc]]
    [:div
      [:h1 "Waiting for first spec to load..."]
      [:p "This may take a second the first time if you call a plot function, unless you first call " [:code '(oz/start-server!)] "."]]))


;; Take anything in the data structure that looks like 

;(defn)

(add-watch chsk-state ::chsk-connected?
  (fn [_ _ _ old-value new-value]
    ;; For some reason new-value v old-value appear to be switched
    ;; So just going to use a fresh deref on the atom value
    (when (:open? @chsk-state)
      (js/console.log "chsk now open; sending connection established message.")
      (go
        (<! (async/timeout 1000))
        (chsk-send! [::connection-established])))))
    ;; TODO Consider whether we should initialize our own reconnection loop, so that we don't necessarily have
    ;; open up a new window when we restart the process (and can reconnect faster). Main downside is if you
    ;; don't realize you still have an old tab open that nabs the session, it won't open a new window; could be
    ;; confusing
    ;; This isn't right... need to have a :reconnecting state separately somewhere since this ends up getting
    ;; triggered in an insane loop due to existing sente reconnection attempts stacking
      ;(do
        ;(js/console.log "oz chsk closed; attempting to reestablish connection")))))
        ;(go
          ;(<! (async/timeout 2000))
          ;(sente/chsk-connect! chsk))))))

(defn init []
  (start-router!)
  (rd/render [error-boundary [application app-state]]
             (. js/document (getElementById "app"))))

(init)
(typeset-mathjax!)

