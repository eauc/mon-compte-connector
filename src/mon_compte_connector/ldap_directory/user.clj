(ns mon-compte-connector.ldap-directory.user
  (:import com.unboundid.util.Base64
           com.unboundid.ldap.sdk.Filter)
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


(defn basic-query
  [{:keys [config schema]} filter]
  (let [user-schema (get-in schema [:user])
        attrs (read-attributes user-schema)]
    {:base-dn (get-in config [:users-base-dn])
     :attributes [:dn]
     :filter filter}))


(defn query
  [{:keys [config schema] :as directory} filter]
  (let [user-schema (get-in schema [:user])
        attrs (read-attributes user-schema)]
    (merge (basic-query directory filter)
           {:attributes (concat (:attributes attrs) (:byte-valued attrs))
            :byte-valued (:byte-valued attrs)})))


(defn base64->binary
  [data attrs]
  (into
    {}
    (map (fn [[k v]]
           (if (get (:attributes attrs) k)
             [(get (:attributes attrs) k) v]
             [(get (:binary-attributes attrs) k) (Base64/decode v)])) data)))


(defn update-query
  [{:keys [dn]} user-schema data]
  (let [attrs (attributes user-schema)
        unknown-keys (clojure.set/difference
                       (set (keys data))
                       (set (concat (keys (dissoc (:attributes attrs) :password :mail))
                                    (keys (:binary-attributes attrs)))))]
    (if-not (empty? unknown-keys)
      (r/create nil (mapv #(format "Unknown data key '%s'" (name %)) unknown-keys))
      (r/just {:dn dn :replace (base64->binary data attrs)
               :post-read (:attributes (read-attributes user-schema))}))))
