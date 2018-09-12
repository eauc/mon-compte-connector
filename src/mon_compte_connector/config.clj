(ns mon-compte-connector.config
  (:import java.security.KeyStore
           javax.crypto.SecretKeyFactory
           javax.crypto.spec.PBEKeySpec)
  (:refer-clojure :exclude [load])
  (:require [cheshire.core :as cs]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [lock-key.core :as lk]
            [mon-compte-connector.cipher :as cipher]
            [mon-compte-connector.keystore :as keystore]))


(defn load-ks
  []
  (try
    (-> (keystore/load)
        (keystore/get-data-entry "config" (:pwd keystore/default-ks)))
    (catch Exception error
      (throw (ex-info "Error loading config from defaut keystore" {:message (.getMessage error)})))))


(defn load
  [{:keys [config-file-path]}]
  (if config-file-path
    (try
      (-> (io/reader config-file-path)
          (cs/parse-stream true))
      (catch Exception error
        (throw (ex-info (format "Error reading config file '%s'" config-file-path)
                        {:message (.getMessage error)}))))
    (load-ks)))


(def default-config
  {:auth {}
   :certs {}
   :admin {}
   :cipher {}
   :directories {}
   :routes {}
   :server {}})


(defn init
  [raw-config {:keys [certs-file-path certs-file-pwd]}]
  (-> (merge default-config raw-config)
      (assoc :certs {:certs-file-path certs-file-path
                     :certs-file-pwd certs-file-pwd})
      (assoc-in [:admin :certs] (ig/ref :certs))
      (assoc-in [:cipher :admin] (ig/ref :admin))
      (assoc-in [:directories :cipher] (ig/ref :cipher))
      (assoc :routes {:admin (ig/ref :admin)
                      :auth (ig/ref :auth)
                      :directories (ig/ref :directories)})
      (assoc-in [:server :certs] (ig/ref :certs))
      (assoc-in [:server :routes] (ig/ref :routes))))


(defn store
  [{:keys [directories] :as config} encrypt]
  (let [encrypted-dirs (encrypt directories)
        storable-config (assoc config :directories {:encrypted encrypted-dirs})]
    (-> (keystore/load-or-create)
        (keystore/set-data-entry "config" (:pwd keystore/default-ks) storable-config)
        (keystore/store))))
