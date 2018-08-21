(ns mon-compte-connector.ldap-directory
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.set :refer [map-invert rename-keys]]
            [clojure.tools.logging :as log]
            [mon-compte-connector.error :as error :refer [->result ->errors err->]]
            [mon-compte-connector.directory :as dir :refer [Directory]]
            [mon-compte-connector.ldap :as ldap]))


(defn user-attributes
  [{:keys [attributes]}]
  (merge {:uid :uid
          :description :description
          :mail :mail
          :phone :phone
          :pwdChangedTime :pwdChangedTime}
         (into {} (map (fn [[k v]] [k (keyword v)]) attributes))))


(defn user-mail-filter
  [user-schema mail]
  (Filter/createANDFilter
    [(Filter/createEqualityFilter (name (get-in user-schema [:attributes :objectclass] :objectclass))
                                  (get user-schema :objectclass "person"))
     (Filter/createEqualityFilter (name (get-in user-schema [:attributes :mail] :mail))
                                  mail)]))


(defn user-uid-filter
  [user-schema uid]
  (Filter/createEqualityFilter (name (get-in user-schema [:attributes :uid] :uid)) uid))


(defn user-query
  [{:keys [config schema]} filter]
  (let [user-schema (get-in schema [:user])]
    {:base-dn (get-in config [:users-base-dn])
     :attributes (vals (user-attributes user-schema))
     :filter filter}))


(defn first-user-found
  [[user]]
  (->result user "user not found"))


(defn user-map-attributes
  [user user-schema]
  (->result (rename-keys user (map-invert (user-attributes user-schema)))))


(defn user
  [{:keys [conn schema] :as directory} filter]
  (let [user-schema (get schema :user)]
    (err-> (user-query directory filter)
           (ldap/search @conn)
           (first-user-found)
           (user-map-attributes user-schema))))


(defn check-user-auth
  [{:keys [dn] :as user} pwd conn]
  (err-> {:dn dn :pwd pwd}
         (ldap/bind? conn)
         (#(if % (->result user) (->errors ["invalid credentials"])))))


(defn authenticated-user
  [{:keys [conn] :as directory} mail pwd]
  (err-> directory
         (user (dir/user-mail-filter directory mail))
         (check-user-auth pwd @conn)))


(defrecord LDAPDirectory [conn config schema]
  Directory
  (dir/user-mail-filter [this mail] (user-mail-filter (get-in this [:schema :user]) mail))
  (dir/user-uid-filter [this uid] (user-uid-filter (get-in this [:schema :user]) uid))
  (dir/user [this filter] (user this filter))
  (dir/authenticated-user [this mail pwd] (authenticated-user this mail pwd)))


(defn make-ldap-directory
  [config schema]
  (let [conn (first (ldap/connect config))]
    (map->LDAPDirectory {:conn (atom conn)
                         :config config
                         :schema schema})))


(defn close
  [{:keys [conn] :as directory}]
  (.close @conn)
  (reset! conn nil))




(comment
  (def mail "user1@myDomain.com")
  (def pwd "Password1")

  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"}})

  (user-attributes user-schema)
  ;; => {:description :description,
  ;;     :mail :mail,
  ;;     :phone :mobile,
  ;;     :pwdChangedTime :pwdChangedTime}

  (user-map-attributes {:description "This is John Doe's description",
                        :mail "user1@myDomain.com",
                        :pwdChangedTime "20180821105506Z",
                        :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                        :mobile "+3312345678"} user-schema)
  ;; => {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;     :description "This is John Doe's description",
  ;;     :mail "user1@myDomain.com",
  ;;     :phone "+3312345678",
  ;;     :pwdChangedTime "20180821105506Z"}
  
  (def config {:host {:address "localhost"
                      :port 636}
               :ssl? true
               :connect-timeout 1000
               :timeout 1000
               :bind-dn "cn=admin,dc=amaris,dc=ovh"
               :password "KLD87cvU"
               :users-base-dn "dc=amaris,dc=ovh"})

  (def directory (make-ldap-directory config {:user user-schema}))

  (-> (dir/user-mail-filter directory mail)
      (.toString))
  ;; => (&(objectclass=person)(mail=user1@myDomain.com))

  (-> (dir/user-uid-filter directory "userUid")
      (.toString))
  ;; => (uid=userUid)
  
  (user-query directory (dir/user-mail-filter directory "toto@acme.com"))
  ;; => {:base-dn "dc=amaris,dc=ovh",
  ;;     :attributes (:description :mail :mobile :pwdChangedTime),
  ;;     :filter
  ;;     #object[com.unboundid.ldap.sdk.Filter 0x6f4581d4 "(&(objectclass=person)(mail=toto@acme.com))"]}

  (dir/user directory (dir/user-mail-filter directory "toto@acme.com"))
  ;; => [nil ["user not found"]]

  (dir/user directory (dir/user-mail-filter directory mail))
  ;; => [{:description "This is John Doe's description",
  ;;      :phone "+3312345678",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwdChangedTime "20180821105506Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"}
  ;;     nil]

  (dir/user directory (dir/user-uid-filter directory "toto"))
  ;; => [nil ["user not found"]]

  (dir/user directory (dir/user-uid-filter directory "JohnD"))
  ;; => [{:description "This is John Doe's description",
  ;;      :phone "+3312345678",
  ;;      :uid "JohnD",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwdChangedTime "20180821105506Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"}
  ;;     nil]  
  
  (dir/authenticated-user directory "toto@acme.com" "Password1")
  ;; => [nil ["user not found"]]

  (dir/authenticated-user directory mail "toto")
  ;; => [nil ("invalid credentials")]

  (dir/authenticated-user directory mail pwd)
  ;; => [{:description "This is John Doe's description",
  ;;      :phone "+3312345678",
  ;;      :uid "JohnD",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwdChangedTime "20180821105506Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"}
  ;;     nil]
  
  (close directory)
  )
