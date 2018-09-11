(ns mon-compte-connector.keystore-dev
  (:import java.util.Collections
           java.security.KeyStore)
  (:require [mon-compte-connector.certs]
            [clojure.java.io :as io]
            [clj-http.client :as client]))


(comment
  
  (def ks
    (let [keystore (KeyStore/getInstance "PKCS12")]
      (with-open [is (io/input-stream "./connector.p12")]
        (.load keystore is (.toCharArray "123456"))
        keystore)))

  (Collections/list (.aliases ks))
  ;; => ["client-cert" "root-ca" "amaris-ca" "server-cert"]

  (.size ks)

  (.getCertificate ks "server-cert")

  (.getCertificate ks "root-ca")
  (.getCertificate ks "amaris-ca")

  (.getCertificateChain ks "client-cert")
  (.getKey ks "client-cert" (.toCharArray "123456"))
  ;; => #object[sun.security.rsa.RSAPrivateCrtKeyImpl 0x39eb635 "sun.security.rsa.RSAPrivateCrtKeyImpl@ffeeb462"]

  (def req-ks
    (doto (KeyStore/getInstance "PKCS12")
      (.load nil nil)))

  (.setKeyEntry req-ks "client-cert"
                (.getKey ks "client-cert" (.toCharArray "123456"))
                (.toCharArray "789456")
                (.getCertificateChain ks "client-cert"))
  (.getCertificate req-ks "client-cert")
  (.getKey req-ks "client-cert" (.toCharArray "789456"))
  (Collections/list (.aliases req-ks))

  (def trust-ks
    (doto (KeyStore/getInstance "PKCS12")
      (.load nil nil)))

  (.setCertificateEntry trust-ks "server-cert" (.getCertificate ks "server-cert"))
  (.getCertificate trust-ks "server-cert")

  ;; (.setCertificateEntry trust-ks "root-ca" (.getCertificate ks "root-ca"))
  ;; (.setCertificateEntry trust-ks "amaris-ca" (.getCertificate ks "amaris-ca"))

  (Collections/list (.aliases trust-ks))

  (client/post "https://k8s.amaris.ovh:30444/v1/connectors/register"
               {:content-type :json
                :as :json
                :keystore req-ks
                ;; :keystore "./connector.p12"
                ;; :keystore-type "pkcs12"
                :keystore-pass "789456"
                :trust-store trust-ks
                })

  (client/post "https://k8s.amaris.ovh:30444/v1/connectors/register"
               {:content-type :json
                :as :json
                :keystore (:keystore (:client (:certs @mon-compte-connector.repl/system)))
                ;; :keystore "./connector.p12"
                ;; :keystore-type "pkcs12"
                :keystore-pass mon-compte-connector.certs/private-pwd
                :trust-store (:trust-store (:client (:certs @mon-compte-connector.repl/system)))
                })


  )
