(ns mon-compte-connector.certs
  (:import java.security.KeyStore)
  (:refer-clojure :exclude [load])
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [mon-compte-connector.keystore :as keystore]))


(defn extract-ks-certs
  [ks pwd]
  (let [client-private-key (keystore/get-key ks "client-cert" pwd)
        client-cert (.getCertificateChain ks "client-cert")
        server-private-key (keystore/get-key ks "server-cert" pwd)
        server-cert (.getCertificateChain ks "server-cert")
        amaris-root-ca (.getCertificate ks "amaris-root-ca")
        amaris-ca (.getCertificate ks "amaris-ca")
        root-ca (.getCertificate ks "root-ca")
        le-ca (.getCertificate ks "le-ca")]
    (cond
      (nil? client-private-key) (throw (ex-info "client private key not found" {}))
      (nil? client-cert) (throw (ex-info "client certificate not found" {}))
      (nil? server-private-key) (throw (ex-info "server private key not found" {}))
      (nil? server-cert) (throw (ex-info "server certificate not found" {}))
      (nil? amaris-root-ca) (throw (ex-info "amaris-root-ca certificate not found" {}))
      (nil? amaris-ca) (throw (ex-info "amaris-ca certificate not found" {}))
      (nil? root-ca) (throw (ex-info "root-ca certificate not found" {}))
      (nil? le-ca) (throw (ex-info "le-ca certificate not found" {}))
      :else {:server-private-key server-private-key
             :server-cert server-cert
             :client-private-key client-private-key
             :client-cert client-cert
             :amaris-root-ca amaris-root-ca
             :amaris-ca amaris-ca
             :root-ca root-ca
             :le-ca le-ca})))


(defn ->client-ks
  [{:keys [client-private-key client-cert amaris-root-ca amaris-ca]} pwd]
  (let [ks (doto (keystore/create)
             (keystore/set-key "client-cert" pwd client-private-key client-cert)
             (.setCertificateEntry "amaris-root-ca" amaris-root-ca)
             (.setCertificateEntry "amaris-ca" amaris-ca))]
    {:keystore ks
     :keystore-pass pwd
     :trust-store ks}))


(defn ->server-ks
  [{:keys [server-private-key server-cert root-ca le-ca]} pwd]
  (let [ks (doto (keystore/create)
             (keystore/set-key "server-cert" pwd server-private-key server-cert)
             (.setCertificateEntry "root-ca" root-ca)
             (.setCertificateEntry "le-ca" le-ca))]
    {:keystore ks
     :keystore-pass pwd} ))


(defn load
  [{:keys [certs-file-path certs-file-pwd] :as certs-options}]
  (try
    (let [certs (-> (keystore/load certs-file-path certs-file-pwd)
                    (extract-ks-certs certs-file-pwd))
          pwd (:pwd keystore/default-ks)]
      {:client (->client-ks certs pwd)
       :server (->server-ks certs pwd)})
    (catch Exception error
      (throw (ex-info (format "Error reading certificates from keystore '%s': %s"
                              certs-file-path (.getMessage error)) {})))))


(defn store
  [{{client-ks :keystore} :client
    {server-ks :keystore} :server}]
  (try
    (let [pwd (:pwd keystore/default-ks)
          client-private-key (keystore/get-key client-ks "client-cert" pwd)
          client-cert (.getCertificateChain client-ks "client-cert")
          server-private-key (keystore/get-key server-ks "server-cert" pwd)
          server-cert (.getCertificateChain server-ks "server-cert")
          root-ca (.getCertificate server-ks "root-ca")
          le-ca (.getCertificate server-ks "le-ca")
          amaris-root-ca (.getCertificate client-ks "amaris-root-ca")
          amaris-ca (.getCertificate client-ks "amaris-ca")]
      (doto (keystore/load-or-create)
        (keystore/set-key "client-cert" pwd client-private-key client-cert)
        (keystore/set-key "server-cert" pwd server-private-key server-cert)
        (.setCertificateEntry "amaris-root-ca" amaris-root-ca)
        (.setCertificateEntry "amaris-ca" amaris-ca)
        (.setCertificateEntry "root-ca" root-ca)
        (.setCertificateEntry "le-ca" le-ca)
        (keystore/store)))
    (catch Exception error
      (throw (ex-info (format "Error writing certificates in keystore '%s'"
                              (:path keystore/default-ks)) {:message (.getMessage error)})))))


(defmethod ig/init-key :certs [_ {:keys [certs-file-path certs-file-pwd] :as config}]
  (let [new-config? (and certs-file-path certs-file-pwd)
        certs (load (if new-config? config {:certs-file-path (:path keystore/default-ks)
                                            :certs-file-pwd (:pwd keystore/default-ks)}))]
    (when new-config? (store certs))
    certs))
