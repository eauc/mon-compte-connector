(ns mon-compte-connector.ldap-directory-test
  (:require [mon-compte-connector.ldap-directory :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.error :as error]))

(deftest ldap-directory-test
  (testing "user-attributes"
    (are [schema attrs] (= attrs (user-attributes schema))

      ;; schema
      {:objectclass "person"
       :attributes {:description "description"
                    :mail "mail"
                    :phone "mobile"}}
      ;; attrs
      {:uid :uid
       :description :description,
       :mail :mail,
       :phone :mobile,
       :pwd-changed-time :pwdChangedTime
       :pwd-policy :pwdPolicySubentry}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; schema
      {:objectclass "person"
       :attributes {:uid "-id"
                    :description "-desc"
                    :mail "-email"
                    :phone "-phone"
                    :pwd-changed-time "-pwdChanged"
                    :pwd-policy "-pwdPolicySubentry"
                    :password "-password"}}
      ;; attrs
      {:uid :-id
       :description :-desc,
       :mail :-email,
       :phone :-phone,
       :pwd-changed-time :-pwdChanged
       :pwd-policy :-pwdPolicySubentry}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; attrs
      {:uid :uid
       :description :description,
       :mail :mail,
       :phone :phone,
       :pwd-changed-time :pwdChangedTime
       :pwd-policy :pwdPolicySubentry}))


  (testing "user-map-attributes"
    (are [raw-user schema user] (= user (first (user-map-attributes raw-user schema)))

      ;; raw-user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"
       :pwdPolicySubentry "cn=pwdPolicy,dn=org,dn=com"}
      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwd-changed-time "user-date"
       :pwd-policy "cn=pwdPolicy,dn=org,dn=com"}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; raw-user
      {:-id "user-uid"
       :-desc "user-desc",
       :-email "user-mail",
       :-phone "user-changed",
       :-pwdChanged "user-date"
       :-pwdPolicy "cn=pwdPolicy,dn=org,dn=com"}
      ;; schema
      {:objectclass "person"
       :attributes {:uid "-id"
                    :description "-desc"
                    :mail "-email"
                    :phone "-phone"
                    :pwd-changed-time "-pwdChanged"
                    :pwd-policy "-pwdPolicy"}}
      ;; user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwd-changed-time "user-date"
       :pwd-policy "cn=pwdPolicy,dn=org,dn=com"}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; raw-user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :mobile "user-changed",
       :pwdChangedTime "user-date"
       :pwdPolicySubentry "cn=pwdPolicy,dn=org,dn=com"}
      ;; schema
      {:objectclass "person"
       :attributes {:phone "mobile"}}
      ;; user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwd-changed-time "user-date"
       :pwd-policy "cn=pwdPolicy,dn=org,dn=com"}))


  (testing "user-mail-filter"
    (are [schema mail filter] (= filter (-> (user-mail-filter schema mail)
                                            (.toString)))

      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; mail
      "toto@acme.com"
      ;; filter
      "(&(objectclass=person)(mail=toto@acme.com))"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; schema
      {:objectclass "user"
       :attributes {:objectclass "class"
                    :mail "email"}}
      ;; mail
      "user1@acme.com"
      ;; filter
      "(&(class=user)(email=user1@acme.com))"))


  (testing "user-uid-filter"
    (are [schema uid filter] (= filter (-> (user-uid-filter schema uid)
                                           (.toString)))

      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; uid filter
      "userUid" "(uid=userUid)"
;;;;;;;;;;;;;;;;;;;;;

      ;; schema
      {:objectclass "user"
       :attributes {:objectclass "class"
                    :uid "id"}}
      ;; uid filter
      "user1" "(id=user1)"))


  (testing "user-query"
    (are [directory filter value query] (= query (-> (user-query
                                                       directory
                                                       (filter (get-in directory [:schema :user]) value))
                                                     (update :filter str)))

      ;; directory
      {:config {:users-base-dn "dc=amaris,dc=ovh"}
       :schema {:user {:objectclass "person"
                       :attributes {}}}}
      ;; filter value
      user-uid-filter "userUid"
      ;; query
      {:base-dn "dc=amaris,dc=ovh",
       :attributes [:uid :description :mail :phone :pwdChangedTime :pwdPolicySubentry],
       :filter "(uid=userUid)"}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; directory
      {:config {:users-base-dn "dc=lvmh,dc=com"}
       :schema {:user {:objectclass "user"
                       :attributes {:description "desc" :mail "email"}}}}
      ;; filter value
      user-mail-filter "user1@lvmh.com"
      ;; query
      {:base-dn "dc=lvmh,dc=com",
       :attributes [:uid :desc :email :phone :pwdChangedTime :pwdPolicySubentry],
       :filter "(&(objectclass=user)(email=user1@lvmh.com))"}))


  (testing "first-user-found"
    (are [users result] (= result (first-user-found users))

      ;; users
      [{:uid "userUid"}
       {:uid "userUid2"}]
      ;; result
      [{:uid "userUid"} nil]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; users
      []
      ;; result
      [nil ["user not found"]]))


  (testing "user-pwd-reset-query"
    (are [user user-schema new-pwd query] (= query (-> (user-pwd-reset-query user user-schema new-pwd)
                                                       error/result))

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
       :post-read '(:-id :description :-mail :phone :pwdChangedTime :pwdPolicySubentry)}))


  (testing "pwd-policy-attributes"
    (are [schema attrs] (= attrs (pwd-policy-attributes schema))

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


  (testing "pwd-policy-map-attributes"
    (are [raw-policy schema policy] (= policy (first (pwd-policy-map-attributes raw-policy schema)))

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


  (testing "pwd-policy-query"
    (are [user config policy-schema query] (= query (pwd-policy-query user config policy-schema))

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


  (testing "pwd-expiration-date"
    (are [policy user policy-schema date] (= date (pwd-expiration-date policy user policy-schema))

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
