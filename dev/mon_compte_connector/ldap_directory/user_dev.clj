(ns mon-compte-connector.ldap-directory.user-dev
  (:require [mon-compte-connector.ldap-directory.user :refer :all]))


(comment
  (def mail "user1@myDomain.com")
  (def pwd "Password1")

  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})

  (attributes user-schema)
  ;; => {:uid :uid,
  ;;     :description :description,
  ;;     :mail :mail,
  ;;     :phone :mobile,
  ;;     :pwd-changed-time :pwdChangedTime,
  ;;     :pwd-policy :pwdPolicySubentry}

  (map-attributes {:description "This is John Doe's description",
                   :mail "user1@myDomain.com",
                   :pwdChangedTime "20180821105506Z",
                   :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
                   :mobile "+3312345678"} user-schema)
  ;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :description "This is John Doe's description",
  ;;      :mail "user1@myDomain.com",
  ;;      :phone "+3312345678",
  ;;      :pwd-changed-time "20180821105506Z"}
  ;;     nil]

  (def config {:users-base-dn "dc=amaris,dc=ovh"})

  (query {:config config :schema {:user user-schema}} "(filter)")
  ;; => {:base-dn "dc=amaris,dc=ovh",
  ;;     :attributes
  ;;     (:uid
  ;;      :description
  ;;      :mail
  ;;      :mobile
  ;;      :pwdChangedTime
  ;;      :pwdPolicySubentry),
  ;;     :filter "(filter)"}
  )
