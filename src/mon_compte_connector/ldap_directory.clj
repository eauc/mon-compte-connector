(ns mon-compte-connector.ldap-directory
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.error :as error :refer [->result ->errors err->]]
            [mon-compte-connector.directory :as dir :refer [Directory]]
            [mon-compte-connector.ldap :as ldap]
            [mon-compte-connector.ldap-directory.filter :as f]
            [mon-compte-connector.ldap-directory.pwd :as p]
            [mon-compte-connector.ldap-directory.pwd-policy :as pp]
            [mon-compte-connector.ldap-directory.user :as u]))


(comment
  (def mail "user1@myDomain.com")
  (def pwd "Password1")

  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})
  )



(defn user-with-pwd-expiration-date
  [user {:keys [conn config schema] :as directory}]
  (let [pwd-policy-schema (get schema :pwd-policy)]
    (err-> user
           (pp/query config pwd-policy-schema)
           (ldap/get @conn)
           (pp/map-attributes pwd-policy-schema)
           (pp/expiration-date user pwd-policy-schema))))

(comment
  (user-with-pwd-expiration-date {} directory)
  ;; => [nil ["invalid pwd expiration date"]]

  (user-with-pwd-expiration-date {:pwd-changed-time "20180821105506Z"} directory)
  ;; => [{:pwd-changed-time "2018-08-21T10:55:06Z",
  ;;      :pwd-max-age "7200",
  ;;      :pwd-expiration-date "2018-08-21T12:55:06Z"}
  ;;     nil]

  )



(defn first-user-found
  [[user]]
  (->result user "user not found"))

(defn user
  [{:keys [conn schema] :as directory} filter]
  (let [user-schema (get schema :user)]
    (err-> (u/query directory filter)
           (ldap/search @conn)
           (first-user-found)
           (u/map-attributes user-schema)
           (user-with-pwd-expiration-date directory))))

(comment
  (dir/user directory (dir/user-mail-filter directory "toto@acme.com"))
  ;; => [nil ["user not found"]]

  (dir/user directory (dir/user-mail-filter directory mail))
  ;; => [{:description "This is John Doe's description",
  ;;      :phone "+3312345678",
  ;;      :uid "JohnD",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwd-changed-time "2018-08-22T19:09:46Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :pwd-max-age "7200",
  ;;      :pwd-expiration-date "2018-08-22T21:09:46Z"}
  ;;     nil]

  (dir/user directory (dir/user-uid-filter directory "toto"))
  ;; => [nil ["user not found"]]

  (dir/user directory (dir/user-uid-filter directory "JohnD"))
  ;; => [{:description "This is John Doe's description",
  ;;      :phone "+3312345678",
  ;;      :uid "JohnD",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwd-changed-time "2018-08-22T19:09:46Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-22T21:09:46Z"}
  ;;     nil]

  )



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

(comment
  (dir/authenticated-user directory "toto@acme.com" "Password1")
  ;; => [nil ["user not found"]]

  (dir/authenticated-user directory mail "toto")
  ;; => [nil ("invalid credentials")]

  (dir/authenticated-user directory mail pwd)
  ;; => [{:description "This is John Doe's description",
  ;;      :phone "+3312345678",
  ;;      :uid "JohnD",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwd-changed-time "2018-08-22T21:17:17Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-22T23:17:17Z"}
  ;;     nil]

  )



(defn user-pwd-reset
  [{:keys [conn schema] :as directory} mail new-pwd]
  (let [user-schema (:user schema)]
    (err-> directory
           (user (dir/user-mail-filter directory mail))
           (p/reset-query user-schema new-pwd)
           (ldap/modify @conn)
           (#(->result (:post-read %)))
           (u/map-attributes user-schema)
           (user-with-pwd-expiration-date directory))))

(comment
  (dir/user-pwd-reset directory "toto@acme.com" "hello")
  ;; => [nil ["user not found"]]

  (dir/user-pwd-reset directory mail "hello")
  ;; => [{:uid "JohnD",
  ;;      :description "This is John Doe's description",
  ;;      :mail "user1@myDomain.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "2018-08-24T14:51:50Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-24T16:51:50Z"}
  ;;     nil]

  )


(defn user-connection
  [{:keys [dn] :as user} pwd pool]
  (let [conn (.getConnection pool)
        ok? (error/result (ldap/bind? {:dn dn :pwd pwd} conn))]
    (if ok?
      (->result [user conn])
      (->errors ["invalid credentials"]))))

(defn pwd-update
  [[user user-conn] {:keys [schema conn]} new-pwd]
  (let [user-schema (:user schema)
        result (err-> user
                      (p/reset-query user-schema new-pwd)
                      (ldap/modify user-conn)
                      (#(->result (:post-read %)))
                      (u/map-attributes user-schema))]
    (.releaseAndReAuthenticateConnection @conn user-conn)
    result))

(defn user-pwd-update
  [{:keys [conn schema] :as directory} mail pwd new-pwd]
  (let [user-schema (:user schema)]
    (err-> directory
           (user (dir/user-mail-filter directory mail))
           (user-connection pwd @conn)
           (pwd-update directory new-pwd)
           (user-with-pwd-expiration-date directory))))

(comment
  (dir/user-pwd-update directory "toto@acme.com" "hello" "Password11")
  ;; => [nil ["user not found"]]

  (dir/user-pwd-update directory mail "hello" "bouh")
  ;; => [nil ["invalid credentials"]]

  (dir/user-pwd-update directory mail "Password11" "bouh")
  ;; => [nil ["Password fails quality checking policy"]]

  (dir/user-pwd-update directory mail "Password12" "Password13")
  ;; => [{:uid "JohnD",
  ;;      :description "This is John Doe's description",
  ;;      :mail "user1@myDomain.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "2018-08-24T18:34:48Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-24T20:34:48Z"}
  ;;     nil]

  )



(defrecord LDAPDirectory [conn config schema]
  Directory
  (dir/user-mail-filter [this mail] (f/user-mail (get-in this [:schema :user]) mail))
  (dir/user-uid-filter [this uid] (f/user-uid (get-in this [:schema :user]) uid))
  (dir/user [this filter] (user this filter))
  (dir/authenticated-user [this mail pwd] (authenticated-user this mail pwd))
  (dir/user-pwd-reset [this mail new-pwd] (user-pwd-reset this mail new-pwd))
  (dir/user-pwd-update [this mail pwd new-pwd] (user-pwd-update this mail pwd new-pwd)))

(defn make-ldap-directory
  [config schema]
  (let [conn (first (ldap/connect config))]
    (map->LDAPDirectory {:conn (atom conn)
                         :config config
                         :schema schema})))

(comment
  (def config {:host {:address "localhost"
                      :port 636}
               :ssl? true
               :connect-timeout 1000
               :timeout 1000
               :bind-dn "cn=admin,dc=amaris,dc=ovh"
               :password "KLD87cvU"
               :users-base-dn "dc=amaris,dc=ovh"
               :default-pwd-policy "cn=passwordDefault,ou=pwpolicies,dc=amaris,dc=ovh"})

  (def directory (make-ldap-directory config {:user user-schema
                                              :pwd-policy {}}))
  )



(defn close
  [{:keys [conn] :as directory}]
  (.close @conn)
  (reset! conn nil))

(comment
  (close directory)
  )
