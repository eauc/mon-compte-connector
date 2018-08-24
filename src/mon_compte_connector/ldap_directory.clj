(ns mon-compte-connector.ldap-directory
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clj-time.core :as time]
            [clj-time.format :as timef]
            [clojure.set :refer [map-invert rename-keys]]
            [clojure.string :refer [replace]]
            [clojure.tools.logging :as log]
            [mon-compte-connector.error :as error :refer [->result ->errors err->]]
            [mon-compte-connector.directory :as dir :refer [Directory]]
            [mon-compte-connector.ldap :as ldap]))


(comment
  (def mail "user1@myDomain.com")
  (def pwd "Password1"))



(defn user-attributes
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
  
  (user-attributes user-schema)
  ;; => {:description :description,
  ;;     :mail :mail,
  ;;     :phone :mobile,
  ;;     :pwdChangedTime :pwdChangedTime}
  )



(defn user-map-attributes
  [user user-schema]
  (->result (rename-keys user (map-invert (user-attributes user-schema)))))

(comment
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
  )



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

(comment
  (-> (dir/user-mail-filter directory mail)
      (.toString))
  ;; => (&(objectclass=person)(mail=user1@myDomain.com))

  (-> (dir/user-uid-filter directory "userUid")
      (.toString))
  ;; => (uid=userUid)
  )


(defn user-read-attributes
  [user-schema]
  (vals (user-attributes user-schema)))

(defn user-query
  [{:keys [config schema]} filter]
  (let [user-schema (get-in schema [:user])]
    {:base-dn (get-in config [:users-base-dn])
     :attributes (user-read-attributes user-schema)
     :filter filter}))

(comment
  (user-query directory (dir/user-mail-filter directory "toto@acme.com"))
  ;; => {:base-dn "dc=amaris,dc=ovh",
  ;;     :attributes (:description :mail :mobile :pwdChangedTime),
  ;;     :filter
  ;;     #object[com.unboundid.ldap.sdk.Filter 0x6f4581d4 "(&(objectclass=person)(mail=toto@acme.com))"]}
  )



(defn pwd-policy-attributes
  [{:keys [attributes]}]
  (->> attributes
       (map (fn [[k v]] [k (keyword v)]))
       (into {})
       (merge {:pwd-max-age :pwdMaxAge})))

(defn pwd-policy-query
  [user config pwd-policy-schema]
  (let [pwd-policy-dn (or (:pwd-policy user) (get config :default-pwd-policy))]
    (->result (when pwd-policy-dn
                {:dn pwd-policy-dn
                 :attributes (vals (pwd-policy-attributes pwd-policy-schema))})
              "missing password policy")))

(comment
  (pwd-policy-query {:pwd-policy "cn=userPwdPolicy,dc=org"}
                    {:default-pwd-policy "cn=defaultPwdPolicy,dc=org"}
                    {:attributes {:pwd-max-age "-pwdMaxAge"}})
  ;; => [{:dn "cn=userPwdPolicy,dc=org", :attributes (:-pwdMaxAge)} nil]
  (pwd-policy-query {}
                    {:default-pwd-policy "cn=defaultPwdPolicy,dc=org"}
                    {:attributes {}})

  ;; => [{:dn "cn=defaultPwdPolicy,dc=org", :attributes (:pwdMaxAge)} nil]

  (pwd-policy-query {}
                    {}
                    {:attributes {:pwd-max-age "-pwdMaxAge"}})
  ;; => [nil ["missing password policy"]]
  
  )



(defn pwd-policy-map-attributes
  [pwd-policy pwd-policy-schema]
  (->result (rename-keys pwd-policy (map-invert (pwd-policy-attributes pwd-policy-schema)))))

(defn format-date-time
  [date-time]
  (timef/unparse (timef/formatters :date-time-no-ms) date-time))

(defn pwd-expiration-date
  [{:keys [pwd-max-age] :as pwd-policy}
   {:keys [pwd-changed-time] :as user}
   {:keys [pwd-changed-time-format] :as pwd-policy-schema
    :or {pwd-changed-time-format "yyyyMMddHHmmssZ"}}]
  (try
    (let [formatter (timef/formatter pwd-changed-time-format)
          pwd-max-age (Integer/parseInt pwd-max-age)
          pwd-changed-time (timef/parse formatter pwd-changed-time)
          pwd-expiration-date (time/plus pwd-changed-time (time/seconds pwd-max-age))]
      (->result (assoc user
                       :pwd-max-age pwd-max-age
                       :pwd-expiration-date (format-date-time pwd-expiration-date)
                       :pwd-changed-time (format-date-time pwd-changed-time))))
    (catch Exception e
      (log/error e "Error calculating pwd expiration date")
      (->errors ["invalid pwd expiration date"]))))

(defn user-with-pwd-expiration-date
  [user {:keys [conn config schema] :as directory}]
  (let [pwd-policy-schema (get schema :pwd-policy)]
    (err-> user
           (pwd-policy-query config pwd-policy-schema)
           (ldap/get @conn)
           (pwd-policy-map-attributes pwd-policy-schema)
           (pwd-expiration-date user pwd-policy-schema))))

(comment
  (pwd-expiration-date {:pwd-max-age "7200"}
                       {:pwd-changed-time "20180821195506Z"}
                       {})
  
  (user-with-pwd-expiration-date {} directory)
  ;; => [nil ["invalid pwd expiration date"]]

  (user-with-pwd-expiration-date {:pwd-changed-time "20180821105506Z"} directory)
  ;; => [{:pwd-changed-time "2018-08-21T10:55:06Z",
  ;;      :pwd-max-age "7200",
  ;;      :pwd-expiration-date "2018-08-21T12:55:06Z"}
  ;;     nil]

  (timef/show-formatters)
  ;;    :date-time-no-ms                        2018-08-22T21:09:32Z
  ;;    :basic-date-time-no-ms                  20180822T210932Z

  )



(defn first-user-found
  [[user]]
  (->result user "user not found"))

(defn user
  [{:keys [conn schema] :as directory} filter]
  (let [user-schema (get schema :user)]
    (err-> (user-query directory filter)
           (ldap/search @conn)
           (first-user-found)
           (user-map-attributes user-schema)
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



(defn user-pwd-reset-query
  [{:keys [dn] :as user} user-schema new-pwd]
  (let [user-pwd-key (-> (get-in user-schema [:attributes :password] "userPassword") keyword)]
    (->result {:dn dn :replace {user-pwd-key new-pwd}
               :post-read (user-read-attributes user-schema)})))

(defn user-pwd-reset
  [{:keys [conn schema] :as directory} mail new-pwd]
  (let [user-schema (:user schema)]
    (err-> directory
           (user (dir/user-mail-filter directory mail))
           (user-pwd-reset-query user-schema new-pwd)
           (ldap/modify @conn)
           (#(->result (:post-read %)))
           (user-map-attributes user-schema)
           (user-with-pwd-expiration-date directory))))

(comment
  (user-pwd-reset-query {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"} user-schema "hello")
;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
;;      :replace {:password "hello"},
;;      :post-read (:uid :description :mail :mobile :pwdChangedTime)}
;;     nil]

  (user-pwd-reset-query {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"}
                        {:attributes {:password "password"}} "hello")
;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
;;      :replace {:password "hello"},
;;      :post-read (:uid :description :mail :phone :pwdChangedTime)}
;;     nil]

  (user-pwd-reset directory "toto@acme.com" "hello")
  ;; => [nil ["user not found"]]

  (user-pwd-reset directory mail "hello")
;; => [{:uid "JohnD",
;;      :description "This is John Doe's description",
;;      :mail "user1@myDomain.com",
;;      :phone "+3312345678",
;;      :pwd-changed-time "2018-08-24T14:51:50Z",
;;      :pwd-max-age 7200,
;;      :pwd-expiration-date "2018-08-24T16:51:50Z"}
;;     nil]
  
  )



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
