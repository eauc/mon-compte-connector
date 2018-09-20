(ns mon-compte-connector.responses-test
  (:require [mon-compte-connector.responses :refer :all]
            [mon-compte-connector.result :as r]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]))


(deftest responses-test
  (testing "user-error"
    (example

      [result? error]

      (= error (user-error result?))

      {:result? (r/create nil ["Token is expired (11871316436)"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["token is expired"]}}}

      {:result? (r/create nil ["Message seems corrupt or tempered with"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["invalid credentials"]}}}

      {:result? (r/create nil ["User not found"
                           "Invalid credentials"
                           "connection error"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["invalid credentials"]}}}

      {:result? (r/create nil ["User not found"
                           "Password does not pass quality checks"
                           "connection error"])
       :error {:status 400
               :body {:status "BadRequest"
                      :messages ["Password does not pass quality checks"]}}}

      {:result? (r/create nil ["User not found"
                           "connection error"
                           "User not found"])
       :error {:status 500
               :body {:status "InternalServerError"
                      :messages ["internal server error"]}}}

      {:result? (r/create nil ["User not found"
                           "User not found"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["invalid credentials"]}}}

      {:result? (r/create nil ["Code is invalid"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["Code is invalid"]}}}

      {:result? (r/create nil ["One-time token is invalid"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["One-time token is invalid"]}}}

      {:result? (r/create nil ["mail is invalid"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["mail is invalid"]}}}))


  (testing "user-token"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :user {:description "This is User11's description",
                          :mail "user11@domain1.com",
                          :phoneNumber "+3312345678",
                          :passwordChangedTime "2018-08-26T12:32:29Z",
                          :passwordMaxAge 7200,
                          :passwordExpirationDate "2018-08-26T14:32:29Z"},
                   :token "eyJhbGciOiJIUzI1NiJ9"}}
           (user-token
             (r/just
               {:user {:description "This is User11's description",
                       :phone "+3312345678",
                       :uid "us11",
                       :mail "user11@domain1.com",
                       :pwd-changed-time "2018-08-26T12:32:29Z",
                       :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
                       :pwd-max-age 7200,
                       :pwd-expiration-date "2018-08-26T14:32:29Z"}
                :token "eyJhbGciOiJIUzI1NiJ9"}))))

    (is (= {:status 401,
            :body {:status "Unauthorized"
                   :messages ["invalid credentials"]}}
           (user-token
             (r/create nil ["User not found"])))))

  (testing "user-info"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :user {:description "This is User11's description",
                          :mail "user11@domain1.com",
                          :phoneNumber "+3312345678",
                          :passwordChangedTime "2018-08-26T12:32:29Z",
                          :passwordMaxAge 7200,
                          :passwordExpirationDate "2018-08-26T14:32:29Z"}}}
           (user-info
             (r/just
               {:description "This is User11's description",
                :phone "+3312345678",
                :uid "us11",
                :mail "user11@domain1.com",
                :pwd-changed-time "2018-08-26T12:32:29Z",
                :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
                :pwd-max-age 7200,
                :pwd-expiration-date "2018-08-26T14:32:29Z"}))))

    (is (= {:status 401,
            :body {:status "Unauthorized"
                   :messages ["invalid credentials"]}}
           (user-info
             (r/create nil ["User not found"])))))


  (testing "user-code"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :code "456123"}}
           (user-code
             (r/just
               {:user {:description "This is User11's description",
                       :phone "+3312345678",
                       :uid "us11",
                       :mail "user11@domain1.com",
                       :pwd-changed-time "2018-08-26T12:32:29Z",
                       :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
                       :pwd-max-age 7200,
                       :pwd-expiration-date "2018-08-26T14:32:29Z"}
                :code "456123"}))))

    (is (= {:status 401,
            :body {:status "Unauthorized"
                   :messages ["invalid credentials"]}}
           (user-code
             (r/create nil ["User not found"])))))


  (testing "user-ott"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :token "eyJhbGciOiJIUzI1NiJ9"}}
           (user-ott
             (r/just
               {:user {:description "This is User11's description",
                       :phone "+3312345678",
                       :uid "us11",
                       :mail "user11@domain1.com",
                       :pwd-changed-time "2018-08-26T12:32:29Z",
                       :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
                       :pwd-max-age 7200,
                       :pwd-expiration-date "2018-08-26T14:32:29Z"}
                :token "eyJhbGciOiJIUzI1NiJ9"}))))

    (is (= {:status 401,
            :body {:status "Unauthorized"
                   :messages ["invalid credentials"]}}
           (user-ott
             (r/create nil ["User not found"])))))

  (testing "user-reset-pwd"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :user {:description "This is User11's description",
                          :mail "user11@domain1.com",
                          :phoneNumber "+3312345678",
                          :passwordChangedTime "2018-08-26T12:32:29Z",
                          :passwordMaxAge 7200,
                          :passwordExpirationDate "2018-08-26T14:32:29Z"}}}
           (user-reset-pwd
             (r/just {:mail "user11@domain1.com"})
             (r/just
               {:description "This is User11's description",
                :phone "+3312345678",
                :uid "us11",
                :mail "user11@domain1.com",
                :pwd-changed-time "2018-08-26T12:32:29Z",
                :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
                :pwd-max-age 7200,
                :pwd-expiration-date "2018-08-26T14:32:29Z"}))))

    (is (= {:status 401,
            :body {:status "Unauthorized"
                   :messages ["token is expired"]}}
           (user-reset-pwd
             (r/create nil ["Token is expired (31486389789)"])
             (r/just
               {:description "This is User11's description",
                :phone "+3312345678",
                :uid "us11",
                :mail "user11@domain1.com",
                :pwd-changed-time "2018-08-26T12:32:29Z",
                :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
                :pwd-max-age 7200,
                :pwd-expiration-date "2018-08-26T14:32:29Z"}))))

    (is (= {:status 401,
            :body {:status "Unauthorized"
                   :messages ["invalid credentials"]}}
           (user-reset-pwd
             (r/just {:mail "user11@domain1.com"})
             (r/create nil ["User not found"]))))))
