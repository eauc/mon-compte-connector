(ns mon-compte-connector.ldap-directory.pwd-policy-test
  (:require [mon-compte-connector.ldap-directory.pwd-policy :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.error :as error]))

(deftest ldap-directory.pwd-policy-test
  (testing "attributes"
    (are [schema attrs]

        (= attrs (attributes schema))

      ;; schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {:pwd-max-age "-pwdMaxAge"}}
      ;; attrs
      {:pwd-max-age :-pwdMaxAge}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {}}
      ;; attrs
      {:pwd-max-age :pwdMaxAge}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ))


  (testing "map-attributes"
    (are [raw-policy schema policy]

        (= policy (first (map-attributes raw-policy schema)))

      ;; raw-policy
      {:-pwdMaxAge "7200"}
      ;; schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {:pwd-max-age "-pwdMaxAge"}}
      ;; policy
      {:pwd-max-age "7200"}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; raw-policy
      {:pwdMaxAge "7200"}
      ;; schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {}}
      ;; policy
      {:pwd-max-age "7200"}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ))


  (testing "query"
    (are [user config policy-schema result]

        (= result (query user config policy-schema))

      ;; user
      {}
      ;; config
      {}
      ;; policy-schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {}}
      ;; query
      [nil ["missing password policy"]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; user
      {}
      ;; config
      {:default-pwd-policy "cn=defaultPwdPolicy,dc=org,dc=com"}
      ;; policy-schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {}}
      ;; query
      [{:dn "cn=defaultPwdPolicy,dc=org,dc=com" :attributes `(:pwdMaxAge)} nil]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; user
      {:pwd-policy "cn=pwdPolicy,dc=org,dc=com"}
      ;; config
      {:default-pwd-policy "cn=defaultPwdPolicy,dc=org,dc=com"}
      ;; policy-schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {:pwd-max-age "-pwdMaxAge"}}
      ;; query
      [{:dn "cn=pwdPolicy,dc=org,dc=com" :attributes `(:-pwdMaxAge)} nil]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ))


  (testing "expiration-date"
    (are [policy user policy-schema date]

        (= date (expiration-date policy user policy-schema))

      ;; policy
      {}
      ;; user
      {}
      ;; policy-schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {}}
      ;; date
      [nil ["invalid pwd expiration date"]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; policy
      {:pwd-max-age "invalid"}
      ;; user
      {:pwd-changed-time "20180817191155Z"}
      ;; policy-schema
      {:attributes {}}
      ;; date
      [nil ["invalid pwd expiration date"]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; policy
      {:pwd-max-age "7200"}
      ;; user
      {:pwd-changed-time "20180817"}
      ;; policy-schema
      {:pwd-changed-time-format "yyyyMMddHHmmssZ"
       :attributes {}}
      ;; date
      [nil ["invalid pwd expiration date"]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; policy
      {:pwd-max-age "7200"}
      ;; user
      {:pwd-changed-time "20180817"}
      ;; policy-schema
      {:pwd-changed-time-format "invalid"
       :attributes {}}
      ;; date
      [nil ["invalid pwd expiration date"]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; policy
      {:pwd-max-age "7200"}
      ;; user
      {:pwd-changed-time "20180817191155Z"}
      ;; policy-schema
      {:attributes {}}
      ;; date
      [{:pwd-changed-time "2018-08-17T19:11:55Z",
        :pwd-max-age 7200,
        :pwd-expiration-date "2018-08-17T21:11:55Z"} nil]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; policy
      {:pwd-max-age "0"}
      ;; user
      {:pwd-changed-time "2018-08-17T19:11:55"}
      ;; policy-schema
      {:pwd-changed-time-format "yyyy-MM-dd'T'HH:mm:ss"
       :attributes {}}
      ;; date
      [{:pwd-changed-time "2018-08-17T19:11:55Z",
        :pwd-max-age 0,
        :pwd-expiration-date "2018-08-17T19:11:55Z"} nil]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      )))
