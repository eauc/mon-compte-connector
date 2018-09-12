(ns mon-compte-connector.ldap-directory.pwd-test
  (:require [mon-compte-connector.ldap-directory.pwd :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]
            [mon-compte-connector.result :as r]))

(deftest ldap-directory.pwd-test
  (testing "reset-query"
    (example

      [user user-schema new-pwd query]

      (= query (-> (reset-query user user-schema new-pwd)
                   r/value))

      {:describe "default user schema"
       :user {:dn "cn=Toto,dc=amaris,dc=ovh"}
       :user-schema {}
       :new-pwd "hello"
       :query {:dn "cn=Toto,dc=amaris,dc=ovh",
               :replace {:userPassword "hello"},
               :post-read '(:uid :description :mail :phone :pwdChangedTime :pwdPolicySubentry)}}

      {:describe "custom user schema"
       :user {:dn "cn=Toto,dc=amaris,dc=ovh"}
       :user-schema {:attributes {:password "-password"
                                  :uid "-id"
                                  :mail "-mail"}}
       :new-pwd "hello"
       :query {:dn "cn=Toto,dc=amaris,dc=ovh",
               :replace {:-password "hello"},
               :post-read '(:-id :description :-mail :phone :pwdChangedTime :pwdPolicySubentry)}})))
