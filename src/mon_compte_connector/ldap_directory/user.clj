(ns mon-compte-connector.ldap-directory.user
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.set :refer [map-invert rename-keys]]
            [clojure.tools.logging :as log]
            [mon-compte-connector.error :as error :refer [->result ->errors err->]]))


(comment
  (def mail "user1@myDomain.com")
  (def pwd "Password1"))



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

(comment
  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})

  (attributes user-schema)
  ;; => {:uid :uid,
  ;;     :description :description,
  ;;     :mail :mail,
  ;;     :phone :mobile,
  ;;     :pwd-changed-time :pwdChangedTime,
  ;;     :pwd-policy :pwdPolicySubentry}
  
  )



(defn map-attributes
  [user user-schema]
  (->result (rename-keys user (map-invert (attributes user-schema)))))

(comment
  (map-attributes {:description "This is John Doe's description",
                   :mail "user1@myDomain.com",
                   :pwdChangedTime "20180821105506Z",
                   :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                   :mobile "+3312345678"} user-schema)
  ;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :description "This is John Doe's description",
  ;;      :mail "user1@myDomain.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "20180821105506Z"}
  ;;     nil]
  
  )


(defn read-attributes
  [user-schema]
  (vals (attributes user-schema)))

(defn query
  [{:keys [config schema]} filter]
  (let [user-schema (get-in schema [:user])]
    {:base-dn (get-in config [:users-base-dn])
     :attributes (read-attributes user-schema)
     :filter filter}))

(comment
  (def config {:users-base-dn "dc=amaris,dc=ovh"})

  (query {:config config :schema {:user user-schema}} "(filter)")
;; => {:base-dn "dc=amaris,dc=ovh",
;;     :attributes
;;     (:uid
;;      :description
;;      :mail
;;      :mobile
;;      :pwdChangedTime
;;      :pwdPolicySubentry),
;;     :filter "(filter)"}
  )
