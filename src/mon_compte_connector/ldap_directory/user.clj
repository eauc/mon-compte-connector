(ns mon-compte-connector.ldap-directory.user
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.set :refer [map-invert rename-keys]]
            [clojure.tools.logging :as log]
            [mon-compte-connector.result :as r]))


(defn attributes
  [{:keys [attributes]}]
  (->> (dissoc attributes :password)
       (map (fn [[k v]] [k (keyword v)]))
       (into {})
       (merge {:uid :uid
               :description :description
               :mail :mail
               :phone :phone
               :pwd-changed-time :pwdChangedTime
               :pwd-policy :pwdPolicySubentry})))


(defn map-attributes
  [user user-schema]
  (r/just (-> user
              (rename-keys (map-invert (attributes user-schema)))
              (dissoc :password))))


(defn read-attributes
  [user-schema]
  (vals (attributes user-schema)))


(defn query
  [{:keys [config schema]} filter]
  (let [user-schema (get-in schema [:user])]
    {:base-dn (get-in config [:users-base-dn])
     :attributes (read-attributes user-schema)
     :filter filter}))
