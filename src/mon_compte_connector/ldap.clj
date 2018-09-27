(ns mon-compte-connector.ldap
  (:import com.unboundid.util.Base64)
  (:refer-clojure :exclude [get])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [clj-ldap.client :as ldap]
            [mon-compte-connector.result :as r :refer [err->]]
            [clojure.string :as str]))


(defn error-message
  [error]
  (let [raw-message (.getMessage error)
        [message] (str/split raw-message #":\s")]
    (or message raw-message)))


(defn log-request
  [fn args]
  (let [label (str fn)]
    (log/info (format "%s request" label) {:args args})
    (let [result (apply fn args)]
      (log/info (format "%s result" label) {:result result})
      result)))


(defn catch-error
  [fn & args]
  (try
    [(log-request fn args) nil]
    (catch Exception error
      (log/warn "LDAP request error" {:message (.getMessage error)})
      (r/create nil [(error-message error)]))))


(defn binary->base64
  [entry]
  (->> entry
       (map (fn [[k v]] [k (if (string? v) v (Base64/encode v))]))
       (into {})))


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


(defn first-search-result
  [[result] error]
  (if (nil? result)
    (r/create nil [error])
    (r/just result)))


(defn search
  [{:keys [base-dn] :as options} conn error]
  (err-> (catch-error ldap/search conn base-dn (dissoc options :base-dn))
         (first-search-result error)
         (#(r/just (binary->base64 %)))))


(defn modify
  [{:keys [dn] :as options} conn]
  (catch-error ldap/modify conn dn (dissoc options :dn)))
