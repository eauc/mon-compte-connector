(ns mon-compte-connector.app
  (:require [clojure.pprint :refer [pprint]]
            [integrant.core :as ig]
            [mon-compte-connector.auth]
            [mon-compte-connector.routes]
            [mon-compte-connector.server]))


(defn start
  [config-file-path]
  (let [config (ig/read-string (slurp config-file-path))]
    (println "Loading config...")
    (pprint config)
    (ig/init config)))
