(def vertx-version "3.8.5")

(defproject delta "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.1.587"]
                 [io.vertx/vertx-core ~vertx-version]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]

  :profiles {:example {:source-paths ["examples"]}}

  :repl-options {:init-ns delta.core})
