(ns mon-compte-connector.auth
  (:import clojure.lang.ExceptionInfo
           java.util.Date)
  (:require [buddy.auth.backends :as backends]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [integrant.core :as ig]
            [one-time.core :as ot]
            [mon-compte-connector.result :as result :refer [->result ->errors]]))


(defn basic-auth
  [request authdata]
  {:mail (:username authdata)
   :pwd (:password authdata)})


(def basic-backend (backends/basic {:authfn basic-auth}))


(defn user-token
  [{:keys [mail uid] :as user} {:keys [now exp-delay secret] :as options}]
  (let [exp (time/plus now exp-delay)
        token (jwt/sign {:mail mail :uid uid :exp exp} secret (dissoc options :secret :now :exp-delay))]
    (->result
      {:user user
       :token token})))


(defn user-claim
  [token {:keys [secret] :as options}]
  (if (empty? token)
    (->errors ["token is invalid"])
    (try
      (->result
        (jwt/unsign token secret (dissoc options :secret)))
      (catch ExceptionInfo e
        (->errors [(.getMessage e)])))))


(defn user-key
  [{:keys [mail]} {:keys [store gen-key]
                   :or {gen-key ot/generate-secret-key}}]
  (or (get @store mail)
      (let [key (gen-key)]
        (swap! store assoc mail key)
        key)))


(defn user-code
  [user {:keys [gen-key store] :as options}]
  (let [code (-> user
                 (user-key options)
                 (ot/get-totp-token (dissoc options :store :gen-key)))]
    (->result
      {:user user
       :code code})))


(def code-invalid "Code is invalid")


(defn user-code-valid?
  [mail code {:keys [store] :as options}]
  (let [valid? (ot/is-valid-totp-token? code (user-key {:mail mail} options)
                                        (dissoc options :store :gen-keys))]
    (if valid?
      (->result mail)
      (->errors [code-invalid]))))


(defn one-time-token
  [mail {:keys [now exp-delay secret store] :as options :or {now (time/now)}}]
  (let [exp (time/plus now exp-delay)
        token (jwt/sign {:mail mail :exp exp} secret (dissoc options :store :secret :now :exp-delay))]
    (swap! store assoc mail token)
    (->result
      {:token token})))


(def ott-invalid "One-time token is invalid")


(defn one-time-claim
  [mail token {:keys [secret store] :as options}]
  (let [claim? (try
                 (->result
                   (jwt/unsign token secret (dissoc options :secret :store)))
                 (catch ExceptionInfo e
                   (->errors [(.getMessage e)])))]
    (if-not (result/ok? claim?)
      claim?
      (let [valid? (= token (get @store mail))]
        (if-not valid?
          (->errors [ott-invalid])
          (do
            (swap! store dissoc mail)
            claim?))))))


(defmethod ig/init-key :auth [_ {:keys [code token] :as config}]
  {:code (assoc code :store (atom {}))
   :token (assoc token
                 :store (atom {})
                 :exp-delay (time/seconds (:exp-delay token)))})
