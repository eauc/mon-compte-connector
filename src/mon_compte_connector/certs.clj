(ns mon-compte-connector.certs
  (:import java.security.KeyStore)
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]))


(def private-pwd "456123")


(defn load
  [{:keys [certs-file-path certs-file-pwd] :as certs-options}]
  (let [throw-error #(throw (ex-info (format "Invalid keystore file: %s" %) certs-options))]
    (try
      (let [keystore (KeyStore/getInstance "PKCS12")]
        (with-open [is (io/input-stream certs-file-path)]
          (.load keystore is (.toCharArray certs-file-pwd))
          (let [private-key (.getKey keystore "server-cert" (.toCharArray certs-file-pwd))
                client-cert (.getCertificateChain keystore "client-cert")
                server-cert (.getCertificateChain keystore "server-cert")
                admin-cert (.getCertificate keystore "admin-cert")]
            (cond
              (nil? private-key) (throw-error "private key not found")
              (nil? client-cert) (throw-error "client certificate not found")
              (nil? server-cert) (throw-error "server certificate not found")
              (nil? admin-cert) (throw-error "admin certificate not found")
              :else {:client {:keystore (doto (KeyStore/getInstance "PKCS12")
                                          (.load nil nil)
                                          (.setKeyEntry "client-cert" private-key
                                                        (.toCharArray private-pwd) client-cert))
                              :keystore-pass private-pwd
                              :trust-store (doto (KeyStore/getInstance "PKCS12")
                                             (.load nil nil)
                                             (.setCertificateEntry "admin-cert" admin-cert))}
                     :server {:keystore (doto (KeyStore/getInstance "PKCS12")
                                          (.load nil nil)
                                          (.setKeyEntry "server-cert" private-key
                                                        (.toCharArray private-pwd) server-cert))
                              :keystore-pass private-pwd}}))))
      (catch Exception error
        (println error)
        (throw-error (.getMessage error))))))


(def default-certs
  {:certs-file-path ".keystore"
   :certs-file-pwd private-pwd})


(defmethod ig/init-key :certs [_ config]
  (load (merge default-certs config)))
