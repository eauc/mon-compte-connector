(ns mon-compte-connector.repl
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [refactor-nrepl.middleware :refer [wrap-refactor]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [integrant.core :as ig]
            [eftest.runner :as eftest]
            [mon-compte-connector.app :as app]))

(defn run-tests
  []
  (eftest/run-tests
    (eftest/find-tests "test")))

(def system (atom nil))

(defn init
  []
  (reset! system (app/init "config.edn")))

(defn reset
  []
  (ig/halt! @system)
  (refresh :after 'mon-compte-connector.repl/init))

(defn -main
  []
  (nrepl-server/start-server :bind "localhost"
                             :port 7888
                             :handler (wrap-refactor cider-nrepl-handler))
  (println "Started nREPL on port 7888")
  (init))
