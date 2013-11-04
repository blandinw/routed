(ns routed.core
  (:require [clojure.string :as string]
            [cljs.reader :as reader]
            [cljs.core.async :as async :refer [<! >! take! put! chan timeout close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

(def gui (js/require "nw.gui"))
(def child_process (js/require "child_process"))
(def util (js/require "util"))
(def fs (js/require "fs"))
(def path (js/require "path"))

(def config (atom {}))
(def state (atom {}))
(def dispatcher (atom nil))

;; -----------------------------------------------------------------------------
;; Utils

(defn format [fmt & args]
  (apply (.-format util) fmt args))

(defn info [msg]
  (.log js/console msg))

(defn infof [& args]
  (info (apply format args)))

;; -----------------------------------------------------------------------------
;; Process

(defn spawn!
  "Given a string, runs it as a shell command"
  [cmd]
  (let [pieces (string/split cmd #"\s+")]
    (.log js/console (str "> Exec: " cmd))
    (.spawn child_process
            (first pieces)
            (apply array (next pieces))
            (clj->js {}))))

(defn kill! [p]
  (.kill p "SIGINT"))

;; -----------------------------------------------------------------------------
;; Config

(defn home-dir
  "Tries to guess user's home dir, even in sudo mode.
   Returns a chan in which home dir will be put."
  []
  (let [out (chan)
        user (or (.-env.SUDO_USER js/process) (.-env.USER js/process))
        cmd (str "id -P " user)
        p (spawn! cmd)]
    (.on (.-stdout p) "data" (fn [data]
                               (let [output (-> (.toString data "UTF-8")
                                                (string/split #":")
                                                (nth 8))]
                                 (.log js/console "Got home dir" output)
                                 (put! out output)
                                 (close! out))))
    out))

(defn read-config!
  "Reads the config, returns a chan containing the config."
  []
  (go (let [home (<! (home-dir))
            path (.resolve path (str home "/.routedrc"))
            contents (.readFileSync fs path (clj->js {:encoding "UTF-8"}))
            conf (reader/read-string contents)]
        (reset! config conf))))

(defn cfg [key]
  (get @config key))

;; -----------------------------------------------------------------------------
;; Mode

(def mode-name first)
(def mode-ip   second)
(def mode-port #(nth % 2))
(def mode-redirect #(nth % 3))

(defn mode [state]
  (:mode state))

(defn next-mode [state]
  (let [current (mode state)
        modes (cfg :modes)]
    (if-not current
      (first modes)
      (->> modes
           cycle
           (drop-while (partial not= current))
           second))))

(defn go-to-next-mode [state]
  (assoc state :mode (next-mode state)))

(defn toggle-mode! []
  (let [state' (swap! state go-to-next-mode)]
    (infof "Toggling mode" (mode-name (mode state')))))

;; -----------------------------------------------------------------------------
;; Dispatcher

(defn say! [msg]
  (spawn! (str "say -v Vicki " msg)))

(defn spawn-dispatcher! [{:keys [mode] :as state}]
  (let [p (spawn! (format "%s %s %s %s %s %s"
                          (cfg :command)
                          (mode-ip mode)
                          (mode-port mode)
                          (cfg :ssl-cert)
                          (cfg :ssl-key)
                          (mode-redirect mode)))]
    (.log js/console "cmd" (format "%s %s %s %s %s %s"
                          (cfg :command)
                          (mode-ip mode)
                          (mode-port mode)
                          (cfg :ssl-cert)
                          (cfg :ssl-key)
                          (mode-redirect mode)))
    (.on (.-stdout p) "data" (fn [data]
                               (.log js/console (str data))))
    (.on (.-stderr p) "data" (fn [data]
                               (.log js/console (str "error: " data))))
    (reset! dispatcher p)
    p))

(defn kill-dispatcher! [state]
  (when-let [d @dispatcher] (kill! d)))

(defn restart-dispatcher! [state]
  (kill-dispatcher! state)
  (spawn-dispatcher! state))

;; -----------------------------------------------------------------------------
;; System Tray

(defn make-menu-item [{:keys [label] :as opts}]
  (let [menu-item-ctor (.-MenuItem gui)
        menu-item (new menu-item-ctor (clj->js opts))]
    (set! (.-label menu-item) label)
    menu-item))

(defn make-menu [opts]
  (let [menu-ctor (.-Menu gui)]
    (new menu-ctor (clj->js opts))))

(defn make-tray [{:keys [title icon menu] :as opts}]
  (let [tray-ctor (.-Tray gui)
        tray (new tray-ctor (clj->js {:title ""}))]
    (set! (.-title tray) title)
    (set! (.-menu tray) menu)
    tray))

(defn make-tray! []
  (when-let [t (:tray @state)] (.remove t))
  (let [sw (make-menu-item {:label "Switch to"})
        _ (set! (.-click sw) (fn [] (toggle-mode!)))
        menu (doto (make-menu {:type "contextmenu"})
               (.append sw))]
    (swap! state assoc
           :tray (make-tray {:title "" :menu menu})
           :switch sw)))

(defn hide-window! []
  (.hide (.Window.get gui)))

(defn watch! []
  (add-watch state
             :change
             (fn [key a old-state state']
               (when (not= (:mode old-state) (:mode state'))
                 (say! (format "Now in %s" (mode-name (mode state'))))
                 (restart-dispatcher! state')
                 (aset (:switch state')
                       "label"
                       (str "Switch to " (mode-name (next-mode state'))))
                 (aset (:tray state')
                       "title"
                       (str (or (cfg :title) "") (mode-name (mode state'))))))))

(defn -main []
  (go (<! (read-config!))
      (watch!)
      (hide-window!)
      (make-tray!)
      (toggle-mode!)))

(-main)
