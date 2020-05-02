(ns example.tcp-connect
  (:require [delta.net :refer :all]
            [clojure.core.async :as a])
  (:import (io.vertx.core Vertx AsyncResult)))


(def vertx (Vertx/vertx))

(defn echo-client []
  (a/go
    (let [client (tcp-client vertx)
          ^AsyncResult r (a/<! (tcp-connect client "127.0.0.1" 9000))]
      (if (.succeeded r)
        (let [net-socket (.result r)
              tcp-chan (tcp-channel net-socket (a/buffer 1024))]
          (prn :connect :ok)
          (loop []
            (when-let [[tk x] (a/<! (a-read! tcp-chan))]
              (case tk
                :buf (do (prn :recv (str x))
                         (a/<! (a-write! tcp-chan x)))
                :err (.printStackTrace ^Throwable x)
                :close (prn :close tk x)
                (prn tk x))
              (recur))))
        (.printStackTrace (.cause r))))))

