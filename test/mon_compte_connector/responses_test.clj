(ns mon-compte-connector.responses-test
  (:require [mon-compte-connector.responses :refer :all]
            [mon-compte-connector.result :refer [->errors ->result]]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]))


(deftest responses-test
  (testing "user-error"
    (example

      [result? error]

      (= error (user-error result?))

      {:result? (->errors ["Token is expired (11871316436)"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["token is expired"]}}}

      {:result? (->errors ["Message seems corrupt or tempered with"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["invalid credentials"]}}}

      {:result? (->errors ["User not found"
                           "Invalid credentials"
                           "connection error"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["invalid credentials"]}}}

      {:result? (->errors ["User not found"
                           "Password does not pass quality checks"
                           "connection error"])
       :error {:status 400
               :body {:status "BadRequest"
                      :messages ["Password does not pass quality checks"]}}}

      {:result? (->errors ["User not found"
                           "connection error"
                           "User not found"])
       :error {:status 500
               :body {:status "InternalServerError"
                      :messages ["internal server error"]}}}

      {:result? (->errors ["User not found"
                           "User not found"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["invalid credentials"]}}}

      {:result? (->errors ["Code is invalid"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["code is invalid"]}}}

      {:result? (->errors ["One-time token is invalid"])
       :error {:status 401
               :body {:status "Unauthorized"
                      :messages ["token is invalid"]}}}))


  (testing "user-token"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :user {:description "This is User11's description",
                          :mail "user11@domain1.com",
                          :phoneNumber "+3312345678",
                          :passwordChangedTime "2018-08-26T12:32:29Z",
                          :passwordMaxAge 7200,
                          :passworddExpirationDate "2018-08-26T14:32:29Z"},
                   :token "eyJhbGciOiJIUzI1NiJ9"}}
           (user-token
             (->result
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
             (->errors ["User not found"])))))

  (testing "user-info"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :user {:description "This is User11's description",
                          :mail "user11@domain1.com",
                          :phoneNumber "+3312345678",
                          :passwordChangedTime "2018-08-26T12:32:29Z",
                          :passwordMaxAge 7200,
                          :passworddExpirationDate "2018-08-26T14:32:29Z"}}}
           (user-info
             (->result
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
             (->errors ["User not found"])))))


  (testing "user-code"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :code "456123"}}
           (user-code
             (->result
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
             (->errors ["User not found"])))))


  (testing "user-ott"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :token "eyJhbGciOiJIUzI1NiJ9"}}
           (user-ott
             (->result
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
             (->errors ["User not found"])))))

  (testing "user-reset-pwd"
    (is (= {:status 200,
            :body {:status "OK"
                   :messages []
                   :user {:description "This is User11's description",
                          :mail "user11@domain1.com",
                          :phoneNumber "+3312345678",
                          :passwordChangedTime "2018-08-26T12:32:29Z",
                          :passwordMaxAge 7200,
                          :passworddExpirationDate "2018-08-26T14:32:29Z"}}}
           (user-reset-pwd
             (->result {:mail "user11@domain1.com"})
             (->result
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
             (->errors ["Token is expired (31486389789)"])
             (->result
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
             (->result {:mail "user11@domain1.com"})
             (->errors ["User not found"]))))))
