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
  (let [config (cfg/config options)]
    (println "Loading config...")
    (pprint config)
    (ig/init config)))
