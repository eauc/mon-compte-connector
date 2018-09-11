(ns mon-compte-connector.certs
  (:import java.security.KeyStore)
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]))


(def private-pwd "456123")


(defn load
  [{:keys [certs-file-path certs-file-pwd]} {:keys [on-error]}]
  (let [print-error #(println (format "Invalid keystore file '%s': %s" certs-file-path %))]
    (try
      (let [keystore (KeyStore/getInstance "PKCS12")]
        (with-open [is (io/input-stream certs-file-path)]
          (.load keystore is (.toCharArray certs-file-pwd))
          (let [client-cert (.getCertificateChain keystore "client-cert")
                private-key (.getKey keystore "client-cert" (.toCharArray certs-file-pwd))
                ;; server-cert (.getCertificate keystore "server-cert")
                ;; server-key (.getKey keystore "server-cert" (.toCharArray certs-file-pwd))
                admin-cert (.getCertificate keystore "admin-cert")]
            (cond
              (nil? client-cert) (print-error "client certificate not found")
              (nil? private-key) (print-error "private key not found")
              (nil? admin-cert) (print-error "admin certificate not found")
              :else {:client {:keystore (doto (KeyStore/getInstance "PKCS12")
                                          (.load nil nil)
                                          (.setKeyEntry "client-cert" private-key
                                                        (.toCharArray private-pwd) client-cert))
                              :keystore-pass private-pwd
                              :trust-store (doto (KeyStore/getInstance "PKCS12")
                                             (.load nil nil)
                                             (.setCertificateEntry "admin-cert" admin-cert))}}))))
      (catch Exception error
        (print-error (.getMessage error))
        (on-error error)))))


(def default-certs
  {:certs-file-path ".keystore"
   :certs-file-pwd private-pwd})


(defmethod ig/init-key :certs [_ config]
  (load (merge default-certs config)
        {:on-error #(throw "Error loading certificates")}))
