(defproject clj-lcoiapi "1.0.0-SNAPSHOT"
  :description "Clojure project for interacting with http://api.lillycoi.com"
  :dependencies [
                 [clj-http "0.4.1"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.2"]
                 [org.apache.opennlp/opennlp-tools "1.5.1-incubating"]
                 [clojure-opennlp "0.1.11-SNAPSHOT"]
                 [noir "1.2.1"]
                ]
  :profiles {:dev {:dependencies [[midje "1.3.1"]]}}
  :jvm-opts ["-Xmx2048m"]
  :main clj-lcoiapi.server)