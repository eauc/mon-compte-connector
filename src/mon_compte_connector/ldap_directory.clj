(ns mon-compte-connector.ldap-directory
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.error :as error :refer [->result ->errors err->]]
            [mon-compte-connector.directory :as dir :refer [Directory DirectoryFilters]]
            [mon-compte-connector.directory-pool :as dir-pool :refer [->DirectoryPool]]
            [mon-compte-connector.ldap :as ldap]
            [mon-compte-connector.ldap-directory.filter :as f]
            [mon-compte-connector.ldap-directory.pwd :as p]
            [mon-compte-connector.ldap-directory.pwd-policy :as pp]
            [mon-compte-connector.ldap-directory.user :as u]))


(comment
  (def domain1 "domain1.com")
  (def user11 {:mail (str "user11@" domain1)
               :pwd "Password11"})
  (def user12 {:mail (str "user12@" domain1)
               :pwd "Password12"})
  (def domain2 "domain2.com")
  (def user21 {:mail (str "user21@" domain2)
               :pwd "Password21"})
  (def user22 {:mail (str "user22@" domain2)
               :pwd "Password22"})

  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})

  (def domain1dc "dc=domain1,dc=com")
  (def config1 {:host {:address "localhost"
                       :port 636}
                :ssl? true
                :connect-timeout 1000
                :timeout 1000
                :bind-dn (str "cn=admin," domain1dc)
                :password "ldap1AdminPwd"
                :users-base-dn (str "ou=Users," domain1dc)
                :default-pwd-policy (str "cn=passwordDefault,ou=pwpolicies," domain1dc)})

  (def directory (make-directory
                   {:config config1
                    :schema {:user user-schema
                             :pwd-policy {}}}))
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
  (dir/user directory #(dir/user-mail-filter % "toto@acme.com"))
  ;; => [nil ["user not found"]]

  (dir/user directory #(dir/user-mail-filter % (:mail user11)))
  ;; => [{:description "This is User11's description",
  ;;      :phone "+3312345678",
  ;;      :uid "us11",
  ;;      :mail "user11@domain1.com",
  ;;      :pwd-changed-time "2018-08-26T12:56:17Z",
  ;;      :dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:56:17Z"}
  ;;     nil]

  (dir/user directory #(dir/user-mail-filter % (:mail user12)))
  ;; => [{:description "This is User12's description",
  ;;      :phone "+3387654321",
  ;;      :uid "us12",
  ;;      :mail "user12@domain1.com",
  ;;      :pwd-changed-time "2018-08-26T12:56:17Z",
  ;;      :dn "cn=User12,ou=Class2,ou=Users,dc=domain1,dc=com",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:56:17Z"}
  ;;     nil]

  (dir/user directory #(dir/user-uid-filter % "toto"))
  ;; => [nil ["user not found"]]

  (dir/user directory #(dir/user-uid-filter % "Us11"))
  ;; => [{:description "This is User11's description",
  ;;      :phone "+3312345678",
  ;;      :uid "us11",
  ;;      :mail "user11@domain1.com",
  ;;      :pwd-changed-time "2018-08-26T12:32:29Z",
  ;;      :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:32:29Z"}
  ;;     nil]

  (dir/user directory #(dir/user-uid-filter % "Us12"))
  ;; => [{:description "This is User12's description",
  ;;      :phone "+3387654321",
  ;;      :uid "us12",
  ;;      :mail "user12@domain1.com",
  ;;      :pwd-changed-time "2018-08-26T12:32:29Z",
  ;;      :dn "cn=User12,ou=Class2,ou=Users,dc=amaris,dc=ovh",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:32:29Z"}
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

  (dir/authenticated-user directory (:mail user11) "toto")
  ;; => [nil ("invalid credentials")]

  (dir/authenticated-user directory (:mail user12) pwd11)
  ;; => [nil ["invalid credentials"]]

  (dir/authenticated-user directory (:mail user11) (:pwd user11))
  ;; => [{:description "This is User11's description",
  ;;      :phone "+3312345678",
  ;;      :uid "us11",
  ;;      :mail "user11@domain1.com",
  ;;      :pwd-changed-time "2018-08-26T12:32:29Z",
  ;;      :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:32:29Z"}
  ;;     nil]

  (dir/authenticated-user directory (:mail user12) (:pwd user12))
  ;; => [{:description "This is User12's description",
  ;;      :phone "+3387654321",
  ;;      :uid "us12",
  ;;      :mail "user12@domain1.com",
  ;;      :pwd-changed-time "2018-08-26T12:32:29Z",
  ;;      :dn "cn=User12,ou=Class2,ou=Users,dc=amaris,dc=ovh",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:32:29Z"}
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

  (dir/user-pwd-reset directory (:mail user11) "hello")
  ;; => [{:uid "us11",
  ;;      :description "This is User11's description",
  ;;      :mail "user11@domain1.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "2018-08-26T12:38:26Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:38:26Z"}
  ;;     nil]

  (dir/user-pwd-reset directory (:mail user12) "world")
  ;; => [{:uid "us12",
  ;;      :description "This is User12's description",
  ;;      :mail "user12@domain1.com",
  ;;      :phone "+3387654321",
  ;;      :pwd-changed-time "2018-08-26T12:38:49Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:38:49Z"}
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

  (dir/user-pwd-update directory (:mail user11) "bah" "bouh")
  ;; => [nil ["invalid credentials"]]

  (dir/user-pwd-update directory (:mail user11) "hello" "bouh")
  ;; => [nil ["Password fails quality checking policy"]]

  (dir/user-pwd-update directory (:mail user11) "hello" "Password11")
  ;; => [nil ["Password is in history of old passwords"]]

  (dir/user-pwd-update directory (:mail user11) "hello" "Password11a")
  ;; => [{:uid "us11",
  ;;      :description "This is User11's description",
  ;;      :mail "user11@domain1.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "2018-08-26T12:40:39Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:40:39Z"}
  ;;     nil]

  (dir/user-pwd-update directory (:mail user12) "world" "Password11")
  ;; => [{:uid "us12",
  ;;      :description "This is User12's description",
  ;;      :mail "user12@domain1.com",
  ;;      :phone "+3387654321",
  ;;      :pwd-changed-time "2018-08-26T12:41:01Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-26T14:41:01Z"}
  ;;     nil]

  )


(defn guard-conn
  [fn {:keys [conn config] :as directory} & args]
  ;; (when (nil? @conn)
  ;;   (reset! conn (first (ldap/connect config))))
  (if (nil? @conn)
    (->errors ["connection failed"])
    (apply fn directory args)))

(defrecord LDAPDirectory [conn config schema]
  DirectoryFilters
  (dir/user-mail-filter [this mail]
    (f/user-mail (get-in this [:schema :user]) mail))
  (dir/user-uid-filter [this uid]
    (f/user-uid (get-in this [:schema :user]) uid))
  Directory
  (dir/user [this filter-fn]
    (guard-conn user this (filter-fn this)))
  (dir/authenticated-user [this mail pwd]
    (guard-conn authenticated-user this mail pwd))
  (dir/user-pwd-reset [this mail new-pwd]
    (guard-conn user-pwd-reset this mail new-pwd))
  (dir/user-pwd-update [this mail pwd new-pwd]
    (guard-conn user-pwd-update this mail pwd new-pwd)))

(defn make-directory
  [{:keys [config schema]}]
  (let [conn (first (ldap/connect config))]
    (map->LDAPDirectory {:conn (atom conn)
                         :config config
                         :schema schema})))



(defn close
  [{:keys [conn] :as directory}]
  (.close @conn)
  (reset! conn nil))

(comment
  (close directory)
  )



(defn make-directory-pool
  [configs]
  (->DirectoryPool
    (map (fn [[k v]] [(name k) (make-directory v)]) configs)))



(comment
  (def domain2dc "dc=domain2,dc=com")
  (def config2 {:host {:address "localhost"
                       :port 637}
                :ssl? true
                :connect-timeout 1000
                :timeout 1000
                :bind-dn (str "cn=admin," domain2dc)
                :password "ldap2AdminPwd"
                :users-base-dn (str "ou=Persons," domain2dc)
                :default-pwd-policy (str "cn=ppDefault,ou=pwpolicies," domain2dc)})

  (def bad-config
    {:host {:address "localhost"
            :port 1636}
     :ssl? true
     :connect-timeout 1000
     :timeout 1000
     :bind-dn "cn=admin,dc=amaris,dc=ovh"
     :password "KLD87cvU"
     :users-base-dn "dc=amaris,dc=ovh"
     :default-pwd-policy "cn=passwordDefault,ou=pwpolicies,dc=amaris,dc=ovh"})

  (def pool (make-directory-pool
              {:server1 {:config config1
                         :schema {:user user-schema
                                  :pwd-policy {}}}
               :bad-server {:config bad-config
                            :schema {:user user-schema
                                     :pwd-policy {}}}
               :server2 {:config config2
                         :schema {:user user-schema
                                  :pwd-policy {}}}}))

  (dir/user pool #(dir/user-uid-filter % "toto"))
  ;; => [nil
  ;;     ("server1: user not found"
  ;;      "bad-server: connection failed"
  ;;      "server2: user not found")]

  (dir/user pool #(dir/user-uid-filter % "Us12"))
  ;; => [["server1"
  ;;      {:description "This is User12's description",
  ;;       :phone "+3387654321",
  ;;       :uid "us12",
  ;;       :mail "user12@domain1.com",
  ;;       :pwd-changed-time "2018-08-26T13:10:14Z",
  ;;       :dn "cn=User12,ou=Class2,ou=Users,dc=domain1,dc=com",
  ;;       :pwd-max-age 7200,
  ;;       :pwd-expiration-date "2018-08-26T15:10:14Z"}]
  ;;     ("bad-server: connection failed" "server2: user not found")]

  (dir/user pool #(dir/user-mail-filter % "user21@domain2.com"))
  ;; => [["server2"
  ;;      {:description "This is User21's description",
  ;;       :phone "+3312345678",
  ;;       :uid "Us21",
  ;;       :mail "user21@domain2.com",
  ;;       :pwd-changed-time "2018-08-26T13:10:14Z",
  ;;       :dn "cn=User21,ou=Class1,ou=Persons,dc=domain2,dc=com",
  ;;       :pwd-max-age 28800,
  ;;       :pwd-expiration-date "2018-08-26T21:10:14Z"}]
  ;;     ("server1: user not found" "bad-server: connection failed")]  

  (dir/authenticated-user pool "toto@titi.fr" "hello123")
  ;; => [nil
  ;;     ("server1: user not found"
  ;;      "bad-server: connection failed"
  ;;      "server2: user not found")]
  
  (dir/authenticated-user pool (:mail user11) "hello123")
  ;; => [nil
  ;;     ("server1: invalid credentials"
  ;;      "bad-server: connection failed"
  ;;      "server2: user not found")]
  
  (dir/authenticated-user pool (:mail user11) "Password11")
  ;; => [["server1"
  ;;      {:description "This is User11's description",
  ;;       :phone "+3312345678",
  ;;       :uid "us11",
  ;;       :mail "user11@domain1.com",
  ;;       :pwd-changed-time "2018-08-26T13:10:14Z",
  ;;       :dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com",
  ;;       :pwd-max-age 7200,
  ;;       :pwd-expiration-date "2018-08-26T15:10:14Z"}]
  ;;     ("bad-server: connection failed" "server2: user not found")]

  (dir/authenticated-user pool "user22@domain2.com" "Password22")
  ;; => [["server2"
  ;;      {:description "This is User22's description",
  ;;       :phone "+3387654321",
  ;;       :uid "Us22",
  ;;       :mail "user22@domain2.com",
  ;;       :pwd-changed-time "2018-08-26T13:10:14Z",
  ;;       :dn "cn=User22,ou=Class3,ou=Persons,dc=domain2,dc=com",
  ;;       :pwd-max-age 28800,
  ;;       :pwd-expiration-date "2018-08-26T21:10:14Z"}]
  ;;     ("server1: user not found" "bad-server: connection failed")]
  
  (dir/user-pwd-reset pool "toto@titi.fr" "hello123")
  ;; => [nil ("bad-server: connection failed" "server: user not found")]

  (dir/user-pwd-reset pool (:mail user11) "hello")
  ;; => [["server1"
  ;;      {:uid "us11",
  ;;       :description "This is User11's description",
  ;;       :mail "user11@domain1.com",
  ;;       :phone "+3312345678",
  ;;       :pwd-changed-time "2018-08-26T13:17:26Z",
  ;;       :pwd-max-age 7200,
  ;;       :pwd-expiration-date "2018-08-26T15:17:26Z"}]
  ;;     ("bad-server: connection failed" "server2: user not found")]

  (dir/user-pwd-reset pool "user22@domain2.com" "world")
  ;; => [["server2"
  ;;      {:uid "Us22",
  ;;       :description "This is User22's description",
  ;;       :mail "user22@domain2.com",
  ;;       :phone "+3387654321",
  ;;       :pwd-changed-time "2018-08-26T13:18:08Z",
  ;;       :pwd-max-age 28800,
  ;;       :pwd-expiration-date "2018-08-26T21:18:08Z"}]
  ;;     ("server1: user not found" "bad-server: connection failed")]  

  (dir/user-pwd-update pool "toto@titi.fr" "hello" "world")
  ;; => [nil
  ;;     ("server1: user not found"
  ;;      "bad-server: connection failed"
  ;;      "server2: user not found")]

  (dir/user-pwd-update pool (:mail user11) "toto" "world")
  ;; => [nil
  ;;     ("server1: invalid credentials"
  ;;      "bad-server: connection failed"
  ;;      "server2: user not found")]

  (dir/user-pwd-update pool (:mail user11) "hello" "world")
  ;; => [nil
  ;;     ("server1: Password fails quality checking policy"
  ;;      "bad-server: connection failed"
  ;;      "server2: user not found")]

  (dir/user-pwd-update pool (:mail user11) "hello" "Password12")
  ;; => [["server1"
  ;;      {:uid "us11",
  ;;       :description "This is User11's description",
  ;;       :mail "user11@domain1.com",
  ;;       :phone "+3312345678",
  ;;       :pwd-changed-time "2018-08-26T13:19:36Z",
  ;;       :pwd-max-age 7200,
  ;;       :pwd-expiration-date "2018-08-26T15:19:36Z"}]
  ;;     ("bad-server: connection failed" "server2: user not found")]

  (dir/user-pwd-update pool "user22@domain2.com" "world" "Password21")
  ;; => [["server2"
  ;;      {:uid "Us22",
  ;;       :description "This is User22's description",
  ;;       :mail "user22@domain2.com",
  ;;       :phone "+3387654321",
  ;;       :pwd-changed-time "2018-08-26T13:20:21Z",
  ;;       :pwd-max-age 28800,
  ;;       :pwd-expiration-date "2018-08-26T21:20:21Z"}]
  ;;     ("server1: user not found" "bad-server: connection failed")]

  (dir/user-pwd-update pool "user22@domain2.com" "world" "Password21")
  ;; => [nil
  ;;     ("server1: An error occurred while attempting to connect to server localhost:636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"
  ;;      "bad-server: connection failed"
  ;;      "server2: An error occurred while attempting to connect to server localhost:637:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:637:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))")]

  )
