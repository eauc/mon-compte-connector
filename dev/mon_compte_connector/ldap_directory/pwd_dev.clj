(ns mon-compte-connector.ldap-directory.pwd-dev
  (:require [mon-compte-connector.ldap-directory.pwd :refer :all]))


(comment
  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})

  (reset-query {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"} user-schema "hello")
  ;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :replace {:password "hello"},
  ;;      :post-read (:uid :description :mail :mobile :pwdChangedTime)}
  ;;     nil]

  (reset-query {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"}
               {:attributes {:password "password"}} "hello")
  ;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :replace {:password "hello"},
  ;;      :post-read (:uid :description :mail :phone :pwdChangedTime)}
  ;;     nil]

  )
