(ns mon-compte-connector.cipher
  (:require [clojure.pprint :refer [pprint]]
            [cheshire.core :as cs]
            [integrant.core :as ig]
            [lock-key.core :as lk]
            [mon-compte-connector.admin :as adm]
            [mon-compte-connector.result :as r :refer [err->]]))



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


(defmethod ig/init-key :cipher [_ {:keys [admin] :as config}]
  (let [secret? (err-> (adm/register admin)
                       (#(r/just (get-in % [:connector :secretKey]))))]
    (if-not (r/ok? secret?)
      (throw (ex-info "Could not register connector in Admin" {:errors (r/logs secret?)}))
      (let [secret (r/value secret?)]
        {:encrypt #(encrypt % secret)
         :decrypt #(decrypt % secret)}))))
