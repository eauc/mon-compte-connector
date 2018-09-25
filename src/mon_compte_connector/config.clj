(ns mon-compte-connector.config
  (:import java.security.KeyStore
           javax.crypto.SecretKeyFactory
           javax.crypto.spec.PBEKeySpec)
  (:refer-clojure :exclude [load])
  (:require [cheshire.core :as cs]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [expound.alpha :as x]
            [integrant.core :as ig]
            [lock-key.core :as lk]
            [mon-compte-connector.cipher :as cipher]
            [mon-compte-connector.keystore :as keystore]))


(x/def ::non-empty-string
  (s/and string? (complement empty?))
  "should be a non-empty string")
(x/def ::port
  nat-int?
  "should be a valid port number")


(x/def :mcc.config.server/http?
  boolean?)
(x/def :mcc.config.server/ssl?
  boolean?)
(x/def :mcc.config.server/port ::port)
(x/def :mcc.config.server/ssl-port ::port)
(x/def :mcc.config.server/client-auth
  #{"need" "want" "none"})
(x/def :mcc.config/server
  (s/keys :req-un [:mcc.config.server/http?
                   :mcc.config.server/ssl?
                   :mcc.config.server/client-auth]
          :opt-un [:mcc.config.server/port
                   :mcc.config.server/ssl-port]))


(x/def :mcc.config.admin/base-url
  (s/and string? #(re-matches #"^https?://.+" %))
  "should be a valid http[s] url")
(x/def :mcc.config/admin
  (s/keys :req-un [:mcc.config.admin/base-url]))


(x/def :mcc.config.auth.code/time-step
  nat-int?
  "should be a time interval in seconds")
(x/def :mcc.config.auth/code
  (s/keys :req-un [:mcc.config.auth.code/time-step]))
(x/def :mcc.config.auth.token/secret
  ::non-empty-string)
(x/def :mcc.config.auth.token/alg
  #{"hs256"})
(x/def :mcc.config.auth.token/exp-delay
  nat-int?
  "should be an expiration delay in seconds")
(x/def :mcc.config.auth/token
  (s/keys :req-un [:mcc.config.auth.token/alg
                   :mcc.config.auth.token/exp-delay
                   :mcc.config.auth.token/secret]))
(x/def :mcc.config/auth
  (s/keys :req-un [:mcc.config.auth/code
                   :mcc.config.auth/token]))


(x/def :mcc.config.directory.host/address
  ::non-empty-string)
(x/def :mcc.config.directory.host/port
  ::port)
(x/def :mcc.config.directory/host
  (s/keys :req-un [:mcc.config.directory.host/address
                   :mcc.config.directory.host/port]))
(x/def :mcc.config.directory/ssl?
  boolean?)
(x/def :mcc.config.directory/connect-timeout
  nat-int?
  "should be a connection timeout in seconds")
(x/def :mcc.config.directory/timeout
  nat-int?
  "should be a request timeout in seconds")
(x/def :mcc.config.directory/bind-dn
  ::non-empty-string
  "should be a admin DN to bind to on connection")
(x/def :mcc.config.directory/password
  string?
  "should be the admin DN password")
(x/def :mcc.config.directory/users-base-dn
  ::non-empty-string
  "should be the base DN to search users entries into")
(x/def :mcc.config.directory/default-pwd-policy
  ::non-empty-string
  "should be the DN of the default password policy")
(x/def :mcc.config.directory/schema
  ::non-empty-string
  "should be the name of the schema to use for users/policies entries")
(x/def :mcc.config/directory
  (s/keys :req-un [:mcc.config.directory/host
                   :mcc.config.directory/ssl?
                   :mcc.config.directory/connect-timeout
                   :mcc.config.directory/timeout
                   :mcc.config.directory/bind-dn
                   :mcc.config.directory/password
                   :mcc.config.directory/users-base-dn
                   :mcc.config.directory/default-pwd-policy]
          :opt-un [:mcc.config.directory/schema]))


(x/def :mcc.config.directory-schema.user/object-class
  ::non-empty-string
  "should be the objectClass of the users entries")
(x/def :mcc.config.directory-schema.user/attributes
  (s/map-of keyword? string?)
  "should be a map of aliases to properties names")
(x/def :mcc.config.directory-schema.user/binary-attributes
  :mcc.config.directory-schema.user/attributes
  "should be a map of aliases to binary properties names")
(x/def :mcc.config.directory-schema.pwd-policy.attributes/pwd-max-age
  ::non-empty-string)
(x/def :mcc.config.directory-schema.pwd-policy/attributes
  (s/keys :opt-un [:mcc.config.directory-schema.pwd-policy.attributes/pwd-max-age]))
(x/def :mcc.config.directory-schema/user
  (s/keys :req-un [:mcc.config.directory-schema.user/object-class]
          :opt-un [:mcc.config.directory-schema.entry/attributes
                   :mcc.config.directory-schema.entry/binary-attributes]))
(x/def :mcc.config.directory-schema/pwd-policy
  (s/keys :opt-un [:mcc.config.directory-schema.pwd-policy/attributes]))
(x/def :mcc.config/directory-schema
  (s/keys :req-un [:mcc.config.directory-schema/pwd-policy
                   :mcc.config.directory-schema/user]))


(x/def :mcc.config.directories/servers
  (s/map-of keyword? :mcc.config/directory))
(x/def :mcc.config.directories/schemas
  (s/map-of keyword? :mcc.config/directory-schema))
(x/def :mcc.config/directories
  (s/keys :req-un [:mcc.config.directories/servers
                   :mcc.config.directories/schemas]))

(x/def :mcc.config/config (s/keys :req-un [:mcc.config/server
                                           :mcc.config/admin
                                           :mcc.config/auth
                                           :mcc.config/directories]))


(defn load-ks
  []
  (try
    (-> (keystore/load)
        (keystore/get-data-entry "config" (:pwd keystore/default-ks)))
    (catch Exception error
      (throw (ex-info "Error loading config from defaut keystore" {:message (.getMessage error)})))))


(defn validate
  [raw-config]
  (log/info "Checking config...")
  (when-not (s/valid? :mcc.config/config raw-config)
    (log/error (x/expound-str :mcc.config/config raw-config))
    (throw (ex-info "Config file does not conform to spec" {})))
  raw-config)


(defn load
  [{:keys [config-file-path]}]
  (if config-file-path
    (try
      (-> (io/reader config-file-path)
          (cs/parse-stream true)
          validate)
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
