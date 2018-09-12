(ns mon-compte-connector.ldap-directory.pwd-policy
  (:require [clj-time.core :as time]
            [clj-time.format :as timef]
            [clojure.set :refer [map-invert rename-keys]]
            [clojure.tools.logging :as log]
            [mon-compte-connector.result :as r]))


(defn attributes
  [{:keys [attributes]}]
  (->> attributes
       (map (fn [[k v]] [k (keyword v)]))
       (into {})
       (merge {:pwd-max-age :pwdMaxAge})))


(defn query
  [user config pwd-policy-schema]
  (let [pwd-policy-dn (or (:pwd-policy user) (get config :default-pwd-policy))]
    (if-not pwd-policy-dn
      (r/create nil ["missing password policy"])
      (r/just {:dn pwd-policy-dn
               :attributes (vals (attributes pwd-policy-schema))}))))


(defn map-attributes
  [pwd-policy pwd-policy-schema]
  (r/just (rename-keys pwd-policy (map-invert (attributes pwd-policy-schema)))))


(defn format-date-time
  [date-time]
  (timef/unparse (timef/formatters :date-time-no-ms) date-time))


(defn expiration-date
  [{:keys [pwd-max-age] :as pwd-policy}
   {:keys [pwd-changed-time] :as user}
   {:keys [pwd-changed-time-format] :as pwd-policy-schema
    :or {pwd-changed-time-format "yyyyMMddHHmmssZ"}}]
  (try
    (let [formatter (timef/formatter pwd-changed-time-format)
          pwd-max-age (Integer/parseInt pwd-max-age)
          pwd-changed-time (timef/parse formatter pwd-changed-time)
          pwd-expiration-date (time/plus pwd-changed-time (time/seconds pwd-max-age))]
      (r/just (assoc user
                     :pwd-max-age pwd-max-age
                     :pwd-expiration-date (format-date-time pwd-expiration-date)
                     :pwd-changed-time (format-date-time pwd-changed-time))))
    (catch Exception e
      (log/error e "Error calculating pwd expiration date")
      (r/create nil ["invalid pwd expiration date"]))))
