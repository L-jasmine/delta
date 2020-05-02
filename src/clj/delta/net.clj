(ns delta.net
  (:require [clojure.core.async :as a])
  (:import (io.vertx.core Vertx Handler)
           (io.vertx.core.buffer Buffer)
           (io.vertx.core.net NetClient NetClientOptions
                              NetServer NetServerOptions NetSocket)))

(defn tcp-client [^Vertx vertx & [opt]]
  (if (instance? NetClientOptions opt)
    (.createNetClient vertx ^NetClientOptions opt)
    (.createNetClient vertx)))

(defprotocol IOChannel
  (a-read! [this])
  (a-write! [this buf]))

(defrecord TcpChannel [^NetSocket socket read-chan]
  IOChannel
  (a-write! [this buf]
    (let [return-chan (a/chan 1)]
      (.write socket ^Buffer buf
              (reify Handler
                (handle [this r]
                  (a/put! return-chan r)
                  (a/close! return-chan))))
      return-chan))
  (a-read! [this] read-chan))

(defn tcp-channel ^TcpChannel [^NetSocket socket read-buffer]
  (let [read-chan (a/chan read-buffer)]
    (.handler socket (reify Handler (handle [this buf] (a/put! read-chan [:buf buf]))))
    (.exceptionHandler socket (reify Handler (handle [this err] (a/put! read-chan [:err err]))))
    (.endHandler socket (reify Handler (handle [this r] (a/put! read-chan [:end]))))
    (.closeHandler
      socket
      (reify Handler (handle [this r]
                       (a/offer! read-chan [:close {:remote (.remoteAddress socket)
                                                    :local  (.localAddress socket)}])
                       (a/close! read-chan))))
    (->TcpChannel socket read-chan)))

(defn tcp-connect [^NetClient client ^String ip ^Number port]
  (let [return-chan (a/chan 1)]
    (.connect client (.intValue port) ^String ip
              (reify Handler
                (handle [this r]
                  (a/put! return-chan r)
                  (a/close! return-chan))))
    return-chan))

(defrecord TcpServer [^NetServer server accept-chan]
  IOChannel
  (a-read! [this] accept-chan))

(defn tcp-server [^Vertx vertx ^Number port accept-buffer & [opt]]
  (a/thread
    (let [server (if (instance? NetServerOptions opt)
                   (.createNetServer vertx ^NetServerOptions opt)
                   (.createNetServer vertx))
          accept-chan (a/chan accept-buffer)]
      (.exceptionHandler server (reify Handler (handle [this err] (a/put! accept-chan [:err err]))))
      (.connectHandler server (reify Handler (handle [this socket] (a/put! accept-chan [:socket socket]))))
      (try
        (->TcpServer (.listen server (.intValue port)) accept-chan)
        (catch Exception e e)))))