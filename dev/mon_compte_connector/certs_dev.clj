(ns mon-compte-connector.certs-dev
  (:require [mon-compte-connector.certs :refer :all]))


(comment

  (def test-certs
    (load {:certs-file-path "certs/connector.p12"
           :certs-file-pwd "123456"}))
  ;; => {:client
  ;;     {:keystore
  ;;      #object[java.security.KeyStore 0x3436aadc "java.security.KeyStore@3436aadc"],
  ;;      :keystore-pass "456123",
  ;;      :trust-store
  ;;      #object[java.security.KeyStore 0x3436aadc "java.security.KeyStore@3436aadc"]},
  ;;     :server
  ;;     {:keystore
  ;;      #object[java.security.KeyStore 0x2958d61e "java.security.KeyStore@2958d61e"],
  ;;      :keystore-pass "456123"}}

  (store test-certs)

  )
