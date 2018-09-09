(ns mon-compte-connector.ldap-directory-test
  (:require [mon-compte-connector.ldap-directory :refer :all]
            [mon-compte-connector.directory :as dir]
            [mon-compte-connector.result :refer [->errors ->result]]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]))

(deftest ldap-directory-test

  (let [mock (fn [calls fn-name result]
               #(do
                  (swap! calls update fn-name (fnil conj []) [%1 %2])
                  result))
        directory-base {:conn (atom (->result "conn"))
                        :config {:users-base-dn "user-base-dn"
                                 :default-pwd-policy "default-pwd-policy"}
                        :schema {:user {:attributes {}}
                                 :pwd-policy {}}}]

    (testing "user"
      (testing "default attributes"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:search (mock calls :search
                                         (->result
                                           [{:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdChangedTime "20180821105506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :phone "+3312345678"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))})]

          (is (= [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                   :description "This is John Doe's description",
                   :mail "user1@myDomain.com",
                   :phone "+3312345678",
                   :pwd-changed-time "2018-08-21T10:55:06Z",
                   :pwd-max-age 7200,
                   :pwd-expiration-date "2018-08-21T12:55:06Z"}
                  nil]
                 (user directory "test-filter")))

          (is (= [[{:base-dn "user-base-dn",
                    :attributes [:uid :description :mail :phone :pwdChangedTime :pwdPolicySubentry],
                    :filter "test-filter"} "conn"]]
                 (:search @calls))
              "should search the user in the ldap")

          (is (= [[{:dn "default-pwd-policy", :attributes [:pwdMaxAge]} "conn"]]
                 (:lget @calls))
              "should retrieve the default pwd policy")))

      (testing "custom attributes"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:schema {:user {:attributes {:phone "mobile"}}
                                    :pwd-policy {:attributes {:pwd-max-age "passwordMaxAge"}}}
                           :search (mock calls :search
                                         (->result
                                           [{:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdPolicySubentry "pwd-policy-user"
                                             :pwdChangedTime "20180821105506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :mobile "+3312345678"}]))
                           :lget (mock calls :lget
                                       (->result {:passwordMaxAge "45800"}))})]

          (is (= [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                   :description "This is John Doe's description",
                   :mail "user1@myDomain.com",
                   :phone "+3312345678",
                   :pwd-changed-time "2018-08-21T10:55:06Z",
                   :pwd-max-age 45800,
                   :pwd-expiration-date "2018-08-21T23:38:26Z"}
                  nil]
                 (user directory "test-filter")))

          (is (= [[{:base-dn "user-base-dn",
                    :attributes [:uid :description :mail :mobile :pwdChangedTime :pwdPolicySubentry],
                    :filter "test-filter"} "conn"]]
                 (:search @calls))
              "should search the user in the ldap")

          (is (= [[{:dn "pwd-policy-user", :attributes [:passwordMaxAge]} "conn"]]
                 (:lget @calls))
              "should retrieve the default pwd policy")))

      (testing "user not found"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:schema {:user {:attributes {:phone "mobile"}}
                                    :pwd-policy {:attributes {:pwd-max-age "passwordMaxAge"}}}
                           :search (mock calls :search
                                         (->result []))
                           :lget (mock calls :lget
                                       (->errors ["object not found"]))})]

          (is (= [nil ["User not found"]]
                 (user directory "test-filter")))))

      (testing "search user error"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:schema {:user {:attributes {:phone "mobile"}}
                                    :pwd-policy {:attributes {:pwd-max-age "passwordMaxAge"}}}
                           :search (mock calls :search
                                         (->errors ["connection error"]))
                           :lget (mock calls :lget
                                       (->result {:passwordMaxAge "45800"}))})]

          (is (= [nil ["connection error"]]
                 (user directory "test-filter")))

          (is (= nil (:lget @calls))
              "should not retrieve the default pwd policy")))

      (testing "pwd policy not found"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:schema {:user {:attributes {:phone "mobile"}}
                                    :pwd-policy {:attributes {:pwd-max-age "passwordMaxAge"}}}
                           :search (mock calls :search
                                         (->result
                                           [{:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdPolicySubentry "pwd-policy-user"
                                             :pwdChangedTime "20180821105506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :mobile "+3312345678"}]))
                           :lget (mock calls :lget (->result nil))})]

          (is (= [nil ["password policy not found"]]
                 (user directory "test-filter")))))

      (testing "pwd policy error"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:schema {:user {:attributes {:phone "mobile"}}
                                    :pwd-policy {:attributes {:pwd-max-age "passwordMaxAge"}}}
                           :search (mock calls :search
                                         (->result
                                           [{:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdPolicySubentry "pwd-policy-user"
                                             :pwdChangedTime "20180821105506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :mobile "+3312345678"}]))
                           :lget (mock calls :lget
                                       (->errors ["object not found"]))})]

          (is (= [nil ["object not found"]]
                 (user directory "test-filter"))))))


    (testing "authenticated-user"
      (testing "success"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdChangedTime "20180821105506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :phone "+3312345678"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :bind? (mock calls :bind?
                                        (->result true))})]

          (is (= [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                   :description "This is John Doe's description",
                   :mail "user1@myDomain.com",
                   :phone "+3312345678",
                   :pwd-changed-time "2018-08-21T10:55:06Z",
                   :pwd-max-age 7200,
                   :pwd-expiration-date "2018-08-21T12:55:06Z"}
                  nil]
                 (authenticated-user directory "user1@myDomain.com" "userPass")))

          (is (= [[directory "user1@myDomain.com"]]
                 (:user-mail-filter @calls))
              "should build the user mail filter")

          (is (= [[{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh", :pwd "userPass"}
                   "conn"]]
                 (:bind? @calls))
              "should try to bind the user")))

      (testing "authent failure"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdChangedTime "20180821105506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :phone "+3312345678"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :bind? (mock calls :bind?
                                        (->errors ["invalid credentials"]))})]

          (is (= [nil [ "invalid credentials"]]
                 (authenticated-user directory "user1@myDomain.com" "userPass"))))))


    (testing "user-pwd-reset"
      (testing "success"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
                                             :pwdChangedTime "20180821105506Z"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :modify (mock calls :modify
                                         (->result
                                           { :post-read
                                            {:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdChangedTime "20180921125506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :phone "+3312345678"}}))})]

          (is (= [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                   :description "This is John Doe's description",
                   :mail "user1@myDomain.com",
                   :phone "+3312345678",
                   :pwd-changed-time "2018-09-21T12:55:06Z",
                   :pwd-max-age 7200,
                   :pwd-expiration-date "2018-09-21T14:55:06Z"}
                  nil]
                 (user-pwd-reset directory "user1@myDomain.com" "newPass")))

          (is (= [[directory "user1@myDomain.com"]]
                 (:user-mail-filter @calls))
              "should build the user mail filter")

          (is (= [[{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                    :replace {:userPassword "newPass"},
                    :post-read
                    [ :uid :description :mail :phone :pwdChangedTime :pwdPolicySubentry]}
                   "conn"]]
                 (:modify @calls))
              "should try to update the user")))

      (testing "update error"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
                                             :pwdChangedTime "20180821105506Z"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :modify (mock calls :modify
                                         (->errors ["invalid update"]))})]

          (is (= [nil ["invalid update"]]
                 (user-pwd-reset directory "user1@myDomain.com" "newPass"))))))


    (testing "user-pwd-update"
      (testing "success"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
                                             :pwdChangedTime "20180821105506Z"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :get-connection #(do
                                              (swap! calls update :get-connection (fnil conj []) [%1])
                                              (->result "user-conn"))
                           :bind? (mock calls :bind?
                                        (->result true))
                           :modify (mock calls :modify
                                         (->result
                                           { :post-read
                                            {:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdChangedTime "20180921125506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :phone "+3312345678"}}))
                           :release-connection (mock calls :release-connection nil)})]

          (is (= [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                   :description "This is John Doe's description",
                   :mail "user1@myDomain.com",
                   :phone "+3312345678",
                   :pwd-changed-time "2018-09-21T12:55:06Z",
                   :pwd-max-age 7200,
                   :pwd-expiration-date "2018-09-21T14:55:06Z"}
                  nil]
                 (user-pwd-update directory "user1@myDomain.com" "oldPass" "newPass")))

          (is (= [[directory "user1@myDomain.com"]]
                 (:user-mail-filter @calls))
              "should build the user mail filter")

          (is (= [["conn"]]
                 (:get-connection @calls))
              "should get a user connection from pool")

          (is (= [[{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh", :pwd "oldPass"}
                   "user-conn"]]
                 (:bind? @calls))
              "should bind user connection using user password")

          (is (= [[{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                    :replace {:userPassword "newPass"},
                    :post-read
                    [ :uid :description :mail :phone :pwdChangedTime :pwdPolicySubentry]}
                   "user-conn"]]
                 (:modify @calls))
              "should try to update the user")

          (is (= [["conn" "user-conn"]]
                 (:release-connection @calls))
              "should release connection")))

      (testing "get user connection error"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
                                             :pwdChangedTime "20180821105506Z"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :get-connection (fn [_] (->errors ["connection error"]))
                           :bind? (mock calls :bind?
                                        (->result true))
                           :modify (mock calls :modify
                                         (->result
                                           { :post-read
                                            {:description "This is John Doe's description",
                                             :mail "user1@myDomain.com",
                                             :pwdChangedTime "20180921125506Z",
                                             :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                                             :phone "+3312345678"}}))
                           :release-connection (mock calls :release-connection nil)})]

          (is (= [nil ["connection error"]]
                 (user-pwd-update directory "user1@myDomain.com" "oldPass" "newPass")))))

      (testing "authent error"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
                                             :pwdChangedTime "20180821105506Z"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :get-connection #(do
                                              (swap! calls update :get-connection (fnil conj []) [%1])
                                              (->result "user-conn"))
                           :bind? (mock calls :bind?
                                        (->errors ["connection error"]))
                           :modify (mock calls :modify
                                         (->result {}))
                           :release-connection (mock calls :release-connection nil)})]

          (is (= [nil ["Invalid credentials"]]
                 (user-pwd-update directory "user1@myDomain.com" "oldPass" "newPass")))))

      (testing "authent failure"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
                                             :pwdChangedTime "20180821105506Z"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :get-connection #(do
                                              (swap! calls update :get-connection (fnil conj []) [%1])
                                              (->result "user-conn"))
                           :bind? (mock calls :bind?
                                        (->result false))
                           :modify (mock calls :modify
                                         (->result {}))
                           :release-connection (mock calls :release-connection nil)})]

          (is (= [nil ["Invalid credentials"]]
                 (user-pwd-update directory "user1@myDomain.com" "oldPass" "newPass")))))

      (testing "update failure"
        (let [calls (atom {})
              directory (merge
                          directory-base
                          {:user-mail-filter (mock calls :user-mail-filter "user-mail-filter")
                           :search (mock calls :search
                                         (->result
                                           [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
                                             :pwdChangedTime "20180821105506Z"}]))
                           :lget (mock calls :lget
                                       (->result {:pwdMaxAge "7200"}))
                           :get-connection #(do
                                              (swap! calls update :get-connection (fnil conj []) [%1])
                                              (->result "user-conn"))
                           :bind? (mock calls :bind?
                                        (->result true))
                           :modify (mock calls :modify
                                         (->errors ["invalid update"]))
                           :release-connection (mock calls :release-connection nil)})]

          (is (= [nil ["invalid update"]]
                 (user-pwd-update directory "user1@myDomain.com" "oldPass" "newPass")))))))


  (testing "first-user-found"
    (example

      [users result]

      (= result (first-user-found users))

      {:describe "found"
       :users [{:uid "userUid"}
               {:uid "userUid2"}]
       :result [{:uid "userUid"} nil]}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      {:describe "not found"
       :users []
       :result [nil ["User not found"]]})))
