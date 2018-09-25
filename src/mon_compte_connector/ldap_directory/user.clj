(ns mon-compte-connector.ldap-directory.user
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.set :refer [map-invert rename-keys]]
            [clojure.tools.logging :as log]
            [mon-compte-connector.result :as r]))


(defn attributes
  [{:keys [attributes binary-attributes]}]
  {:attributes (->> (dissoc attributes :password)
                    (map (fn [[k v]] [k (keyword v)]))
                    (into {})
                    (merge {:uid :uid
                            :description :description
                            :mail :mail
                            :phone :phone
                            :pwd-changed-time :pwdChangedTime
                            :pwd-policy :pwdPolicySubentry}))
   :binary-attributes (->> (dissoc binary-attributes :password)
                           (map (fn [[k v]] [k (keyword v)]))
                           (into {})
                           (merge {:photo :photo}))})


(defn map-attributes
  [user user-schema]
  (let [attrs (attributes user-schema)]
    (r/just (-> user
                (rename-keys (map-invert (merge (:attributes attrs)
                                                (:binary-attributes attrs))))
                (dissoc :password)))))


(defn read-attributes
  [user-schema]
  (let [{:keys [attributes binary-attributes]} (attributes user-schema)]
    {:attributes (vals attributes)
     :byte-valued (vals binary-attributes)}))


(defn query
  [{:keys [config schema]} filter]
  (let [user-schema (get-in schema [:user])
        attrs (read-attributes user-schema)]
    {:base-dn (get-in config [:users-base-dn])
     :attributes (concat (:attributes attrs) (:byte-valued attrs))
     :byte-valued (:byte-valued attrs)
     :filter filter}))
