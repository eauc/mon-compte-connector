(ns mon-compte-connector.ldap-directory.pwd-policy-test
  (:require [mon-compte-connector.ldap-directory.pwd-policy :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]))

(deftest ldap-directory.pwd-policy-test
  (testing "attributes"
    (example

      [schema attrs]

      (= attrs (attributes schema))

      {:describe "custom attributes"
       :schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                :attributes {:pwd-max-age "-pwdMaxAge"}}
       :attrs {:pwd-max-age :-pwdMaxAge}}

      {:describe "default attributes"
       :schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                :attributes {}}
       :attrs {:pwd-max-age :pwdMaxAge}}

      ))


  (testing "map-attributes"
    (example

      [raw-policy schema policy]

      (= policy (first (map-attributes raw-policy schema)))

      {:describe "custom attributes"
       :raw-policy {:-pwdMaxAge "7200"}
       :schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                :attributes {:pwd-max-age "-pwdMaxAge"}}
       :policy {:pwd-max-age "7200"}}

      {:describe "default attributes"
       :raw-policy {:pwdMaxAge "7200"}
       :schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                :attributes {}}
       :policy {:pwd-max-age "7200"}}

      ))


  (testing "query"
    (example

      [user config policy-schema result]

      (= result (query user config policy-schema))

      {:describe "default attributes"
       :user {}
       :config {}
       :policy-schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                       :attributes {}}
       :result [nil ["missing password policy"]]}

      {:describe "custom attributes"
       :user {}
       :config {:default-pwd-policy "cn=defaultPwdPolicy,dc=org,dc=com"}
       :policy-schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                       :attributes {}}
       :result [{:dn "cn=defaultPwdPolicy,dc=org,dc=com" :attributes `(:pwdMaxAge)} nil]}

      {:describe "custom pwd policy for user"
       :user {:pwd-policy "cn=pwdPolicy,dc=org,dc=com"}
       :config {:default-pwd-policy "cn=defaultPwdPolicy,dc=org,dc=com"}
       :policy-schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                       :attributes {:pwd-max-age "-pwdMaxAge"}}
       :result [{:dn "cn=pwdPolicy,dc=org,dc=com" :attributes `(:-pwdMaxAge)} nil]}

      ))


  (testing "expiration-date"
    (example

      [policy user policy-schema date]

      (= date (expiration-date policy user policy-schema))

      {:describe "default attributes"
       :policy {}
       :user {}
       :policy-schema {:pwd-changed-time-format "yyyyMMddHHmmssZ"
                       :attributes {}}
       :date [nil ["invalid pwd expiration date"]]}

      {:describe "invalid pwd max age"
       :policy {:pwd-max-age "invalid"}
       :user {:pwd-changed-time "20180817191155Z"}
       :policy-schema {:attributes {}}
       :date [nil ["invalid pwd expiration date"]]}

      {:describe "invalid pwd change time value"
       :policy {:pwd-max-age "7200"}
       :user {:pwd-changed-time "20180817"}
       :policy-schema
       {:pwd-changed-time-format "yyyyMMddHHmmssZ"
        :attributes {}}
       :date [nil ["invalid pwd expiration date"]]}

      {:describe "invalid pwd change time format"
       :policy {:pwd-max-age "7200"}
       :user {:pwd-changed-time "20180817"}
       :policy-schema {:pwd-changed-time-format "invalid"
                       :attributes {}}
       :date [nil ["invalid pwd expiration date"]]}

      {:describe "success"
       :policy {:pwd-max-age "7200"}
       :user {:pwd-changed-time "20180817191155Z"}
       :policy-schema {:attributes {}}
       :date [{:pwd-changed-time "2018-08-17T19:11:55Z",
               :pwd-max-age 7200,
               :pwd-expiration-date "2018-08-17T21:11:55Z"} nil]}

      {:describe "success - max age null"
       :policy {:pwd-max-age "0"}
       :user {:pwd-changed-time "2018-08-17T19:11:55"}
       :policy-schema {:pwd-changed-time-format "yyyy-MM-dd'T'HH:mm:ss"
                       :attributes {}}
       :date [{:pwd-changed-time "2018-08-17T19:11:55Z",
               :pwd-max-age 0,
               :pwd-expiration-date "2018-08-17T19:11:55Z"} nil]}

      )))
