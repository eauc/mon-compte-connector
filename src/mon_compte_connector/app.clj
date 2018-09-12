(ns mon-compte-connector.app
  (:require [clojure.pprint :refer [pprint]]
            [integrant.core :as ig]
            [mon-compte-connector.auth]
            [mon-compte-connector.admin]
            [mon-compte-connector.certs]
            [mon-compte-connector.config :as cfg]
            [mon-compte-connector.ldap-directory]
            [mon-compte-connector.routes]
            [mon-compte-connector.server]))


(defn start
  [options]
  (let [raw-config (cfg/load options)]
    (println "Loading config...")
    (pprint raw-config)
    (let [system (ig/init (cfg/init raw-config options))]
      (when (nil? (get-in raw-config [:directories :encrypted]))
        (cfg/store raw-config (get-in system [:admin :secret])))
      system)))
