(ns mon-compte-connector.test
  (:import clojure.lang.ExceptionInfo)
  (:require [clj-ldap.client :as ldap]
            [clj-http.client :as client]
            [clj-time.core :as time]
            [mon-compte-connector.auth :as auth]
            [mon-compte-connector.directory :as dir]
            [mon-compte-connector.event-log :as event-log]
            [mon-compte-connector.ldap-directory :as ld]
            [mon-compte-connector.result :as result :refer [->result ->errors err->]]
            [clojure.tools.logging :as log]
            [clojure.set :as set]))

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

    (def pool (result/value (ld/make-directory-pool
                              {:server1 {:config config1
                                         :schema {:user user-schema
                                                  :pwd-policy {}}}
                               :bad-server {:config bad-config
                                            :schema {:user user-schema
                                                     :pwd-policy {}}}
                               :server2 {:config config2
                                         :schema {:user user-schema
                                                  :pwd-policy {}}}}))))

  (def auth-options
    {:now (time/now)
     :exp-delay (time/years 1)
     :secret "mySecret"})


  (def token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjExQGRvbWFpbjEuY29tIiwidWlkIjoidXMxMSIsImV4cCI6MTU2NzcxNDk4Mn0.fmb3F5FN7_I6f7GKvGz9sw9EHM3Xz6KdkAWX2GahtlI"))

