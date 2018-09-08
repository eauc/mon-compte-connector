(ns mon-compte-connector.repl
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.spec.alpha :as s]
            [eftest.runner :as eftest]
            [expound.alpha :as expound]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]
            [mon-compte-connector.core]
            [mon-compte-connector.app :as app]))


(defn run-tests
  []
  (eftest/run-tests
    (eftest/find-tests "test")))


(def system (atom nil))


(defn start
  []
  (reset! system (app/start "config.dev.edn")))


(defn reset
  []
  (ig/halt! @system)
  (refresh :after 'mon-compte-connector.repl/start))


(defn init
  []
  (alter-var-root #'s/*explain-out* (constantly expound/printer))
  (st/instrument)
  (start))
