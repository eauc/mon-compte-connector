(ns mon-compte-connector.app
  (:require [clojure.pprint :refer [pprint]]
            [integrant.core :as ig]
            [mon-compte-connector.auth]
            [mon-compte-connector.admin]
            [mon-compte-connector.certs]
            [mon-compte-connector.config :as cfg]
            [mon-compte-connector.debug :as dbg]
            [mon-compte-connector.ldap-directory]
            [mon-compte-connector.routes]
            [mon-compte-connector.server]))


(defn start
  [options]
  (let [raw-config (cfg/load options)]
    (dbg/println "Loading config...")
    (dbg/pprint raw-config)
    (let [system (try
                   (ig/init (cfg/init raw-config options))
                   (catch clojure.lang.ExceptionInfo ex
                     (ig/halt! (:system (ex-data ex)))
                     (throw (.getCause ex))))]
      (when (nil? (get-in raw-config [:directories :encrypted]))
        (cfg/store raw-config (get-in system [:cipher :encrypt])))
      system)))
