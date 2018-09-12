(ns mon-compte-connector.auth
  (:import clojure.lang.ExceptionInfo
           java.util.Date)
  (:require [buddy.auth.backends :as backends]
            [buddy.sign.jwt :as jwt]
            [clojure.pprint :refer [pprint]]
            [clj-time.core :as time]
            [integrant.core :as ig]
            [one-time.core :as ot]
            [mon-compte-connector.result :as r]))


(defn basic-auth
  [request authdata]
  {:mail (:username authdata)
   :pwd (:password authdata)})


(def basic-backend (backends/basic {:authfn basic-auth}))


(defn user-token
  [{:keys [mail uid] :as user} {:keys [now exp-delay secret] :as options}]
  (let [exp (time/plus now exp-delay)
        sign-options (dissoc options :secret :now :exp-delay :store)
        token (jwt/sign {:mail mail :uid uid :exp exp} secret sign-options)]
    (r/just {:user user
             :token token})))


(defn user-claim
  [token {:keys [secret] :as options}]
  (if (empty? token)
    (r/create nil ["token is invalid"])
    (try
      (r/just (jwt/unsign token secret (dissoc options :secret)))
      (catch ExceptionInfo e
        (r/create nil [(.getMessage e)])))))


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
    (r/just {:user user
             :code code})))


(def code-invalid "Code is invalid")


(defn user-code-valid?
  [mail code {:keys [store] :as options}]
  (let [valid? (ot/is-valid-totp-token? code (user-key {:mail mail} options)
                                        (dissoc options :store :gen-keys))]
    (if valid?
      (r/just mail)
      (r/create nil [code-invalid]))))


(defn one-time-token
  [mail {:keys [now exp-delay secret store] :as options :or {now (time/now)}}]
  (let [exp (time/plus now exp-delay)
        token (jwt/sign {:mail mail :exp exp} secret (dissoc options :store :secret :now :exp-delay))]
    (swap! store assoc mail token)
    (r/just {:token token})))


(def ott-invalid "One-time token is invalid")


(defn one-time-claim
  [mail token {:keys [secret store] :as options}]
  (let [claim? (try
                 (r/just (jwt/unsign token secret (dissoc options :secret :store)))
                 (catch ExceptionInfo e
                   (r/create nil [(.getMessage e)])))]
    (if-not (r/ok? claim?)
      claim?
      (let [valid? (= token (get @store mail))]
        (if-not valid?
          (r/create nil [ott-invalid])
          (do
            (swap! store dissoc mail)
            claim?))))))


(def users-keys-store (atom {}))
(def ott-store (atom {}))


(defmethod ig/init-key :auth [_ {:keys [code token] :as config}]
  (let [base-code (assoc code :store users-keys-store)
        base-token (-> token
                       (update :alg keyword)
                       (assoc :store ott-store
                              :exp-delay (time/seconds (:exp-delay token))))]
    {:code #(assoc base-code :date (Date.))
     :token #(assoc base-token :now (time/now))}))
