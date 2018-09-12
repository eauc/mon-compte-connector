(ns mon-compte-connector.keystore
  (:import java.security.KeyStore
           javax.crypto.SecretKeyFactory
           javax.crypto.spec.PBEKeySpec)
  (:refer-clojure :exclude [load])
  (:require [cheshire.core :as cs]
            [clojure.java.io :as io]))


(def private-pwd "456123")



(def default-ks
  {:path ".keystore"
   :pwd private-pwd})


(defn create
  []
  (doto (KeyStore/getInstance "PKCS12")
    (.load nil nil)))


(defn load
  ([path pwd]
   (with-open [is (io/input-stream path)]
     (doto (KeyStore/getInstance "PKCS12")
       (.load is (.toCharArray pwd)))))
  ([]
   (load (:path default-ks) (:pwd default-ks))))



(defn load-or-create
  ([path pwd]
   (if (.exists (io/file path))
     (load path pwd)
     (create)))
  ([]
   (load-or-create (:path default-ks) (:pwd default-ks))))


(defn store
  ([ks path pwd]
   (with-open [os (io/output-stream path)]
     (.store ks os (.toCharArray pwd))
     ks))
  ([ks]
   (store ks (:path default-ks) (:pwd default-ks))))


(defn set-key
  [ks alias pwd key cert]
  (.setKeyEntry ks alias key (.toCharArray pwd) cert)
  ks)


(defn get-key
  [ks alias pwd]
  (.getKey ks alias (.toCharArray pwd)))


(defn set-data-entry
  [ks alias pwd data]
  (let [data-key-spec (-> data
                          cs/generate-string
                          .toCharArray
                          PBEKeySpec.)
        pwd-protection (-> pwd
                           .toCharArray
                           java.security.KeyStore$PasswordProtection.)
        key-entry (-> (SecretKeyFactory/getInstance "PBE")
                      (.generateSecret data-key-spec)
                      java.security.KeyStore$SecretKeyEntry.)]
    (.setEntry ks alias key-entry pwd-protection))
  ks)


(defn get-data-entry
  [ks alias pwd]
  (let [pwd-protection (-> pwd
                           .toCharArray
                           java.security.KeyStore$PasswordProtection.)
        secret-key (-> ks
                       (.getEntry alias pwd-protection)
                       .getSecretKey)
        key-spec (-> (SecretKeyFactory/getInstance "PBE")
                     (.getKeySpec secret-key (Class/forName "javax.crypto.spec.PBEKeySpec")))
        data-string (String. (.getPassword ^PBEKeySpec key-spec))]
    (cs/parse-string data-string true)))
