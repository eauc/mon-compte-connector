(ns mon-compte-connector.cipher
  (:require [cheshire.core :as cs]
            [lock-key.core :as lk]))



(defn encrypt
  [data secret]
  (-> data
      cs/generate-string
      (lk/encrypt-as-base64 secret)))


(defn decrypt
  [encrypted secret]
  (-> encrypted
      (lk/decrypt-from-base64 secret)
      cs/parse-string true))
