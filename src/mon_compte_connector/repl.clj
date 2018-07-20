(ns mon-compte-connector.repl
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [refactor-nrepl.middleware :refer [wrap-refactor]]
            [eftest.runner :as eftest]
            [mon-compte-connector.core :as app]))

(defn run-tests
  []
  (eftest/run-tests
    (eftest/find-tests "test")))

(defn -main
  [& args]
  (println "Starting REPL on port 7888")
  (nrepl-server/start-server :bind "localhost"
                             :port 7888
                             :handler (wrap-refactor cider-nrepl-handler))
  (apply app/-main args))
