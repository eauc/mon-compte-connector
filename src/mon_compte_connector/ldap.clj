(ns mon-compte-connector.ldap
  (:import com.unboundid.ldap.sdk.Filter)
  (:refer-clojure :exclude [get])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [clj-ldap.client :as ldap]
            [mon-compte-connector.result :as r]
            [clojure.string :as str]))


(defn error-message
  [error]
  (let [raw-message (.getMessage error)
        [message] (str/split raw-message #":\s")]
    (or message raw-message)))


(defn catch-error
  [fn & args]
  (try
    [(apply fn args) nil]
    (catch Exception error
      (log/error error "LDAP request error")
      (r/create nil [(error-message error)]))))



(def connect (partial catch-error ldap/connect))


(defn get-connection
  [conn]
  (catch-error #(.getConnection conn)))


(defn release-connection
  [pool conn]
  (.releaseAndReAuthenticateConnection pool conn))


(defn bind?
  [{:keys [dn pwd]} conn]
  (catch-error ldap/bind? conn dn pwd))


(defn get
  [{:keys [dn attributes]} conn]
  (catch-error ldap/get conn dn attributes))


(defn search
  [{:keys [base-dn] :as options} conn]
  (catch-error #(or (ldap/search conn base-dn (dissoc options :base-dn)) '())))


(defn modify
  [{:keys [dn] :as options} conn]
  (catch-error ldap/modify conn dn (dissoc options :dn)))
