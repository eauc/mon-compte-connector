(ns mon-compte-connector.keystore-dev
  (:import java.util.Collections
           java.security.KeyStore)
  (:require [mon-compte-connector.certs]
            [clojure.java.io :as io]
            [clj-http.client :as client]))


(comment
  
  (def ks
    (let [keystore (KeyStore/getInstance "PKCS12")]
      (with-open [is (io/input-stream "./certs/connector.p12")]
        (.load keystore is (.toCharArray "123456"))
        keystore)))

  (def ks
    (let [keystore (KeyStore/getInstance "PKCS12")]
      (with-open [is (io/input-stream "../../mon-compte-root/connector/data/acme.combined.p12")]
        (.load keystore is (.toCharArray "MT9zoRrKZ2ZfMzUb29f2nVpz"))
        keystore)))

  (Collections/list (.aliases ks))
  ;; => ["client-cert" "root-ca" "amaris-ca" "server-cert"]

  (.size ks)

  (.getCertificate ks "server-cert")

  (.getCertificate ks "ca-chain")

  (.getCertificate ks "root-ca")
  
  (.getCertificate ks "amaris-root-ca")
  
  (.getCertificate ks "amaris-ca")

  (.getCertificateChain ks "client-cert")
  (.getKey ks "client-cert" (.toCharArray "123456"))
  ;; => #object[sun.security.rsa.RSAPrivateCrtKeyImpl 0x39eb635 "sun.security.rsa.RSAPrivateCrtKeyImpl@ffeeb462"]

  (def req-ks
    (doto (KeyStore/getInstance "PKCS12")
      (.load nil nil)))

  (.setKeyEntry req-ks "client-cert"
                (.getKey ks "client-cert" (.toCharArray "123456"))
                (.toCharArray "456123")
                (.getCertificateChain ks "client-cert"))
  (.getCertificate req-ks "client-cert")
  (.setCertificateEntry req-ks "amaris-root-ca" (.getCertificate ks "amaris-root-ca"))
  (.setCertificateEntry req-ks "amaris-ca" (.getCertificate ks "amaris-ca"))
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
                :keystore-pass "456123"
                :trust-store req-ks
                })

  (def test-certs
    (:client (:certs @mon-compte-connector.repl/system)))

  (def test-ks
    (:keystore test-certs))

  (Collections/list (.aliases test-ks))

  (.getCertificate test-ks "client-cert")
  (.getKey test-ks "client-cert" (.toCharArray "456123"))

  (client/post "https://k8s.amaris.ovh:30444/v1/connectors/register"
               {:content-type :json
                :as :json
                :keystore test-ks
                ;; :keystore "./connector.p12"
                ;; :keystore-type "pkcs12"
                :keystore-pass "456123"
                :trust-store test-ks
                })
  
  (client/post "https://k8s.amaris.ovh:30444/v1/connectors/register"
               {:content-type :json
                :as :json
                :keystore (:keystore (:client (:certs @mon-compte-connector.repl/system)))
                ;; :keystore "./connector.p12"
                ;; :keystore-type "pkcs12"
                :keystore-pass mon-compte-connector.certs/private-pwd
                :trust-store (:keystore (:client (:certs @mon-compte-connector.repl/system)))
                })


  )
