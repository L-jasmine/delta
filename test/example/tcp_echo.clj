(ns example.tcp-echo
  (:require [clojure.core.async :as a]
            [delta.net :refer :all])
  (:import (io.vertx.core Vertx)
           (io.vertx.core.net NetSocket)))

(def vertx (Vertx/vertx))

(defn socket-handler [s]
  (a/go
    (prn :accept s)
    (let [chan (tcp-channel ^NetSocket s (a/buffer 1024))]
      (loop []
        (when-let [[tk x] (a/<! (a-read! chan))]
          (case tk
            :buf (do (prn :recv (str x))
                     (a/<! (a-write! chan x)))
            :err (.printStackTrace ^Throwable x)
            :close (prn :close tk x)
            (prn tk x))
          (recur))))
    (prn :stop s)))

(defn echo-server []
  (a/go
    (let [server (a/<! (tcp-server vertx 9000 (a/buffer 1000)))]
      (prn :start 9000)
      (loop []
        (let [[tk x] (a/<! (a-read! server))]
          (case tk
            :err (.printStackTrace ^Throwable x)
            :socket
            (do
              (socket-handler x)
              (recur))
            [tk x]))))))