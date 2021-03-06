(ns mon-compte-connector.ldap-directory-dev
  (:require [mon-compte-connector.ldap-directory :refer :all]
            [mon-compte-connector.directory :as dir :refer [Directory DirectoryFilters]]
            [mon-compte-connector.directory-pool :as dir-pool]
            [mon-compte-connector.result :as r]))


(comment
  (do
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

    (def directory (r/value
                     (make-directory
                       {:config config1
                        :schema {:user user-schema
                                 :pwd-policy {}}}))))

  (user-with-pwd-expiration-date {} directory)
  ;; => [nil ["invalid pwd expiration date"]]

  (user-with-pwd-expiration-date {:pwd-changed-time "20180821105506Z"} directory)
  ;; => [{:pwd-changed-time "2018-08-21T10:55:06Z",
  ;;      :pwd-max-age "7200",
  ;;      :pwd-expiration-date "2018-08-21T12:55:06Z"}
  ;;     nil]

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

  (dir/authenticated-user directory "toto@acme.com" "Password1")
  ;; => [nil ["user not found"]]

  (dir/authenticated-user directory (:mail user11) "toto")
  ;; => [nil ("invalid credentials")]

  (dir/authenticated-user directory (:mail user12) (:pwd user11))
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

  (close directory)



  (do
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

    (def pool (r/value (make-directory-pool
                         {:server1 {:config config1
                                    :schema {:user user-schema
                                             :pwd-policy {}}}
                          :bad-server {:config bad-config
                                       :schema {:user user-schema
                                                :pwd-policy {}}}
                          :server2 {:config config2
                                    :schema {:user user-schema
                                             :pwd-policy {}}}}))))

  (dir/user pool #(dir/user-uid-filter % "toto"))
  ;; => [nil
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user pool #(dir/user-uid-filter % "Us12"))
  ;; => [{:description "This is User12's description",
  ;;      :phone "+3387654321",
  ;;      :uid "us12",
  ;;      :server "server1",
  ;;      :mail "user12@domain1.com",
  ;;      :pwd-changed-time "2018-09-06T21:40:50Z",
  ;;      :dn "cn=User12,ou=Class2,ou=Users,dc=domain1,dc=com",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-09-06T23:40:50Z"}
  ;;     ("bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user pool #(dir/user-mail-filter % "user21@domain2.com"))
  ;; => [{:description "This is User21's description",
  ;;      :phone "+3312345678",
  ;;      :uid "Us21",
  ;;      :server "server2",
  ;;      :mail "user21@domain2.com",
  ;;      :pwd-changed-time "2018-09-06T21:40:50Z",
  ;;      :dn "cn=User21,ou=Class1,ou=Persons,dc=domain2,dc=com",
  ;;      :pwd-max-age 28800,
  ;;      :pwd-expiration-date "2018-09-07T05:40:50Z"}
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636")]

  (dir/authenticated-user pool "toto@titi.fr" "hello123")
  ;; => [nil
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/authenticated-user pool (:mail user11) "hello123")
  ;; => [nil
  ;;     ("server1: Invalid credentials"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/authenticated-user pool (:mail user11) "Password11")
  ;; => [{:description "This is User11's description",
  ;;      :phone "+3312345678",
  ;;      :uid "us11",
  ;;      :server "server1",
  ;;      :mail "user11@domain1.com",
  ;;      :pwd-changed-time "2018-09-06T21:40:50Z",
  ;;      :dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-09-06T23:40:50Z"}
  ;;     ("bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/authenticated-user pool "user22@domain2.com" "Password22")
  ;; => [{:description "This is User22's description",
  ;;      :phone "+3387654321",
  ;;      :uid "Us22",
  ;;      :server "server2",
  ;;      :mail "user22@domain2.com",
  ;;      :pwd-changed-time "2018-09-06T21:40:50Z",
  ;;      :dn "cn=User22,ou=Class3,ou=Persons,dc=domain2,dc=com",
  ;;      :pwd-max-age 28800,
  ;;      :pwd-expiration-date "2018-09-07T05:40:50Z"}
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636")]

  (dir/user-pwd-reset pool "toto@titi.fr" "hello123")
  ;; => [nil
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user-pwd-reset pool (:mail user11) "hello")
  ;; => [{:uid "us11",
  ;;      :description "This is User11's description",
  ;;      :mail "user11@domain1.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "2018-09-06T22:00:08Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-09-07T00:00:08Z",
  ;;      :server "server1"}
  ;;     ("bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user-pwd-reset pool "user22@domain2.com" "world")
  ;; => [{:uid "Us22",
  ;;      :description "This is User22's description",
  ;;      :mail "user22@domain2.com",
  ;;      :phone "+3387654321",
  ;;      :pwd-changed-time "2018-09-06T22:00:25Z",
  ;;      :pwd-max-age 28800,
  ;;      :pwd-expiration-date "2018-09-07T06:00:25Z",
  ;;      :server "server2"}
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636")]

  (dir/user-pwd-update pool "toto@titi.fr" "hello" "world")
  ;; => [nil
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user-pwd-update pool (:mail user11) "toto" "world")
  ;; => [nil
  ;;     ("server1: Invalid credentials"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user-pwd-update pool (:mail user11) "hello" "world")
  ;; => [nil
  ;;     ("server1: Password fails quality checking policy"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user-pwd-update pool (:mail user11) "hello" "Password12")
  ;; => [{:uid "us11",
  ;;      :description "This is User11's description",
  ;;      :mail "user11@domain1.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "2018-09-06T22:01:48Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-09-07T00:01:48Z",
  ;;      :server "server1"}
  ;;     ("bad-server: An error occurred while attempting to connect to server localhost:1636"
  ;;      "server2: User not found")]

  (dir/user-pwd-update pool "user22@domain2.com" "world" "Password21")
  ;; => [{:uid "Us22",
  ;;      :description "This is User22's description",
  ;;      :mail "user22@domain2.com",
  ;;      :phone "+3387654321",
  ;;      :pwd-changed-time "2018-09-06T22:02:12Z",
  ;;      :pwd-max-age 28800,
  ;;      :pwd-expiration-date "2018-09-07T06:02:12Z",
  ;;      :server "server2"}
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636")]

  (dir-pool/close pool)

  )
