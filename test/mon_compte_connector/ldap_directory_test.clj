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
       :pwdChangedTime :pwdChangedTime}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      
      ;; schema
      {:objectclass "person"
       :attributes {:uid "-id"
                    :description "-desc"
                    :mail "-email"
                    :phone "-phone"
                    :pwdChangedTime "-pwdChanged"
                    :password "-password"}}
      ;; attrs
      {:uid :-id
       :description :-desc,
       :mail :-email,
       :phone :-phone,
       :pwdChangedTime :-pwdChanged}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      
      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; attrs
      {:uid :uid
       :description :description,
       :mail :mail,
       :phone :phone,
       :pwdChangedTime :pwdChangedTime}))


  (testing "user-map-attributes"
    (are [raw-user schema user] (= user (first (user-map-attributes raw-user schema)))

      ;; raw-user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}
      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      
      ;; raw-user
      {:-id "user-uid"
       :-desc "user-desc",
       :-email "user-mail",
       :-phone "user-changed",
       :-pwdChanged "user-date"}
      ;; schema
      {:objectclass "person"
       :attributes {:uid "-id"
                    :description "-desc"
                    :mail "-email"
                    :phone "-phone"
                    :pwdChangedTime "-pwdChanged"}}
      ;; user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      
      ;; raw-user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :mobile "user-changed",
       :pwdChangedTime "user-date"}
      ;; schema
      {:objectclass "person"
       :attributes {:phone "mobile"}}
      ;; user
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}))


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
       :attributes [:uid :description :mail :phone :pwdChangedTime],
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
       :attributes [:uid :desc :email :phone :pwdChangedTime],
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
       :post-read '(:uid :description :mail :phone :pwdChangedTime)}
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
       :post-read '(:-id :description :-mail :phone :pwdChangedTime)})))
