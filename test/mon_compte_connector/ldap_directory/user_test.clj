(ns mon-compte-connector.ldap-directory.user-test
  (:require [mon-compte-connector.ldap-directory.user :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]))

(deftest ldap-directory.user-test
  (testing "attributes"
    (example

      [schema attrs]

      (= attrs (attributes schema))

      {:describe "partial attributes"
       :schema {:objectclass "person"
                :attributes {:description "description"
                             :mail "mail"
                             :phone "mobile"}}
       :attrs {:uid :uid
               :description :description,
               :mail :mail,
               :phone :mobile,
               :pwd-changed-time :pwdChangedTime
               :pwd-policy :pwdPolicySubentry}}

      {:describe "custom attributes"
       :schema {:objectclass "person"
                :attributes {:uid "-id"
                             :description "-desc"
                             :mail "-email"
                             :phone "-phone"
                             :pwd-changed-time "-pwdChanged"
                             :pwd-policy "-pwdPolicySubentry"
                             :password "-password"}}
       :attrs {:uid :-id
               :description :-desc,
               :mail :-email,
               :phone :-phone,
               :pwd-changed-time :-pwdChanged
               :pwd-policy :-pwdPolicySubentry}}

      {:describe "default attributes"
       :schema {:objectclass "person"
                :attributes {}}
       :attrs {:uid :uid
               :description :description,
               :mail :mail,
               :phone :phone,
               :pwd-changed-time :pwdChangedTime
               :pwd-policy :pwdPolicySubentry}}))


  (testing "map-attributes"
    (example

      [raw-user schema user]

      (= user (first (map-attributes raw-user schema)))

      {:describe "default attributes"
       :raw-user {:uid "user-uid"
                  :description "user-desc",
                  :mail "user-mail",
                  :phone "user-changed",
                  :pwdChangedTime "user-date"
                  :pwdPolicySubentry "cn=pwdPolicy,dn=org,dn=com"}
       :schema {:objectclass "person"
                :attributes {}}
       :user {:uid "user-uid"
              :description "user-desc",
              :mail "user-mail",
              :phone "user-changed",
              :pwd-changed-time "user-date"
              :pwd-policy "cn=pwdPolicy,dn=org,dn=com"}}

      {:describe "custom attributes"
       :raw-user {:-id "user-uid"
                  :-desc "user-desc",
                  :-email "user-mail",
                  :-phone "user-changed",
                  :-pwdChanged "user-date"
                  :-pwdPolicy "cn=pwdPolicy,dn=org,dn=com"}
       :schema {:objectclass "person"
                :attributes {:uid "-id"
                             :description "-desc"
                             :mail "-email"
                             :phone "-phone"
                             :pwd-changed-time "-pwdChanged"
                             :pwd-policy "-pwdPolicy"}}
       :user {:uid "user-uid"
              :description "user-desc",
              :mail "user-mail",
              :phone "user-changed",
              :pwd-changed-time "user-date"
              :pwd-policy "cn=pwdPolicy,dn=org,dn=com"}}

      {:describe "partial attributes"
       :raw-user {:uid "user-uid"
                  :description "user-desc",
                  :mail "user-mail",
                  :mobile "user-changed",
                  :pwdChangedTime "user-date"
                  :pwdPolicySubentry "cn=pwdPolicy,dn=org,dn=com"}
       :schema {:objectclass "person"
                :attributes {:phone "mobile"}}
       :user {:uid "user-uid"
              :description "user-desc",
              :mail "user-mail",
              :phone "user-changed",
              :pwd-changed-time "user-date"
              :pwd-policy "cn=pwdPolicy,dn=org,dn=com"}}))


  (testing "query"
    (example

      [directory filter result]

      (= result (query directory filter))

      {:describe "default attributes"
       :directory {:config {:users-base-dn "dc=amaris,dc=ovh"}
                   :schema {:user {:objectclass "person"
                                   :attributes {}}}}
       :filter "(uid=userUid)"
       :result {:base-dn "dc=amaris,dc=ovh",
                :attributes [:uid :description :mail :phone :pwdChangedTime :pwdPolicySubentry],
                :filter "(uid=userUid)"}}

      {:describe "custom attributes"
       :directory {:config {:users-base-dn "dc=lvmh,dc=com"}
                   :schema {:user {:objectclass "user"
                                   :attributes {:description "desc" :mail "email"}}}}
       :filter "(&(objectclass=user)(email=user1@lvmh.com))"
       :result {:base-dn "dc=lvmh,dc=com",
                :attributes [:uid :desc :email :phone :pwdChangedTime :pwdPolicySubentry],
                :filter "(&(objectclass=user)(email=user1@lvmh.com))"}})))
