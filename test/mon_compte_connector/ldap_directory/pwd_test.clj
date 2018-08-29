(ns mon-compte-connector.ldap-directory.pwd-test
  (:require [mon-compte-connector.ldap-directory.pwd :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.result :as result]))

(deftest ldap-directory.pwd-test
  (testing "reset-query"
    (are [user user-schema new-pwd query]

        (= query (-> (reset-query user user-schema new-pwd)
                     result/value))

      ;; user
      {:dn "cn=Toto,dc=amaris,dc=ovh"}
      ;; user-schema
      {}
      ;; new-pwd
      "hello"
      ;; query
      {:dn "cn=Toto,dc=amaris,dc=ovh",
       :replace {:userPassword "hello"},
       :post-read '(:uid :description :mail :phone :pwdChangedTime :pwdPolicySubentry)}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; user
      {:dn "cn=Toto,dc=amaris,dc=ovh"}
      ;; user-schema
      {:attributes {:password "-password"
                    :uid "-id"
                    :mail "-mail"}}
      ;; new-pwd
      "hello"
      ;; query
      {:dn "cn=Toto,dc=amaris,dc=ovh",
       :replace {:-password "hello"},
       :post-read '(:-id :description :-mail :phone :pwdChangedTime :pwdPolicySubentry)})))
