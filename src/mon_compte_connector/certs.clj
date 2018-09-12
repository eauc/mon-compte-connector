(ns mon-compte-connector.certs
  (:import java.security.KeyStore)
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [mon-compte-connector.keystore :as keystore]))


(defn extract-ks-certs
  [ks pwd throw-error]
  (let [private-key (keystore/get-key ks "server-cert" pwd)
        client-cert (.getCertificateChain ks "client-cert")
        server-cert (.getCertificateChain ks "server-cert")
        root-ca (.getCertificate ks "root-ca")
        amaris-ca (.getCertificate ks "amaris-ca")]
    (cond
      (nil? private-key) (throw-error "private key not found")
      (nil? client-cert) (throw-error "client certificate not found")
      (nil? server-cert) (throw-error "server certificate not found")
      (nil? root-ca) (throw-error "root-ca certificate not found")
      (nil? amaris-ca) (throw-error "amaris-ca certificate not found")
      :else {:private-key private-key
             :server-cert server-cert
             :client-cert client-cert
             :root-ca root-ca
             :amaris-ca amaris-ca})))


(defn ->client-ks
  [{:keys [private-key client-cert root-ca amaris-ca]} pwd]
  (let [ks (doto (keystore/create)
             (keystore/set-key "client-cert" pwd private-key client-cert)
             (.setCertificateEntry "root-ca" root-ca)
             (.setCertificateEntry "amaris-ca" amaris-ca))]
    {:keystore ks
     :keystore-pass pwd
     :trust-store ks}))


(defn ->server-ks
  [{:keys [private-key server-cert root-ca amaris-ca]} pwd]
  (let [ks (doto (keystore/create)
             (keystore/set-key "server-cert" pwd private-key server-cert)
             (.setCertificateEntry "root-ca" root-ca)
             (.setCertificateEntry "amaris-ca" amaris-ca))]
    {:keystore ks
     :keystore-pass pwd} ))


(defn load
  [{:keys [certs-file-path certs-file-pwd] :as certs-options}]
  (let [throw-error #(throw (ex-info (format "Invalid keystore file '%s': %s" certs-file-path %) certs-options))
        certs (-> (keystore/load certs-file-path certs-file-pwd)
                  (extract-ks-certs certs-file-pwd throw-error))
        pwd (:pwd keystore/default-ks)]
    {:client (->client-ks certs pwd)
     :server (->server-ks certs pwd)}))


(defn store
  [{{client-ks :keystore} :client
    {server-ks :keystore} :server}]
  (let [pwd (:pwd keystore/default-ks)
        private-key (keystore/get-key server-ks "server-cert" pwd)
        client-cert (.getCertificateChain client-ks "client-cert")
        server-cert (.getCertificateChain server-ks "server-cert")
        root-ca (.getCertificate server-ks "root-ca")
        amaris-ca (.getCertificate server-ks "amaris-ca")]
    (doto (keystore/load-or-create)
      (keystore/set-key "client-cert" pwd private-key client-cert)
      (keystore/set-key "server-cert" pwd private-key server-cert)
      (.setCertificateEntry "root-ca" root-ca)
      (.setCertificateEntry "amaris-ca" amaris-ca)
      (keystore/store))))


(defmethod ig/init-key :certs [_ {:keys [certs-file-path certs-file-pwd] :as config}]
  (let [new-config? (and certs-file-path certs-file-pwd)
        certs (load (if new-config? config {:certs-file-path (:path keystore/default-ks)
                                            :certs-file-pwd (:pwd keystore/default-ks)}))]
    (when new-config? (store certs))
    certs))
