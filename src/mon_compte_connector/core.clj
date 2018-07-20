(ns mon-compte-connector.core
  (:require [mon-compte-connector.app :refer [init]]))

(defn -main [config-file-path]
  (init config-file-path))
