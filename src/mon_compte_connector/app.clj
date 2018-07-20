(ns mon-compte-connector.app
  (:require [mon-compte-connector.server]
            [integrant.core :as ig]))

(defn init
  [config-file-path]
  (let [config (ig/read-string (slurp config-file-path))]
    (println "Loading config" config)
    (ig/init config)))
