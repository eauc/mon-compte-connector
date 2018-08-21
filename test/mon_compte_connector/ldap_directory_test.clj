(ns mon-compte-connector.ldap-directory-test
  (:require [mon-compte-connector.ldap-directory :refer :all]
            [clojure.test :refer [deftest testing is are]]))

(deftest ldap-directory-test
  (testing "user-attributes"
    (are [schema attrs] (= attrs (user-attributes schema))

      {:objectclass "person"
       :attributes {:description "description"
                    :mail "mail"
                    :phone "mobile"}}
      {:uid :uid
       :description :description,
       :mail :mail,
       :phone :mobile,
       :pwdChangedTime :pwdChangedTime}

      {:objectclass "person"
       :attributes {:uid "-id"
                    :description "-desc"
                    :mail "-email"
                    :phone "-phone"
                    :pwdChangedTime "-pwdChanged"}}
      {:uid :-id
       :description :-desc,
       :mail :-email,
       :phone :-phone,
       :pwdChangedTime :-pwdChanged}

      {:objectclass "person"
       :attributes {}}
      {:uid :uid
       :description :description,
       :mail :mail,
       :phone :phone,
       :pwdChangedTime :pwdChangedTime}))


  (testing "user-map-attributes"
    (are [raw-user schema user] (= user (first (user-map-attributes raw-user schema)))

      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}
      {:objectclass "person"
       :attributes {}}
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}

      {:-id "user-uid"
       :-desc "user-desc",
       :-email "user-mail",
       :-phone "user-changed",
       :-pwdChanged "user-date"}
      {:objectclass "person"
       :attributes {:uid "-id"
                    :description "-desc"
                    :mail "-email"
                    :phone "-phone"
                    :pwdChangedTime "-pwdChanged"}}
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}

      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :mobile "user-changed",
       :pwdChangedTime "user-date"}
      {:objectclass "person"
       :attributes {:phone "mobile"}}
      {:uid "user-uid"
       :description "user-desc",
       :mail "user-mail",
       :phone "user-changed",
       :pwdChangedTime "user-date"}))


  (testing "user-mail-filter"
    (are [schema mail filter] (= filter (-> (user-mail-filter schema mail)
                                            (.toString)))

      {:objectclass "person"
       :attributes {}}
      "toto@acme.com"
      "(&(objectclass=person)(mail=toto@acme.com))"

      {:objectclass "user"
       :attributes {:objectclass "class"
                    :mail "email"}}
      "user1@acme.com"
      "(&(class=user)(email=user1@acme.com))"))


  (testing "user-uid-filter"
    (are [schema uid filter] (= filter (-> (user-uid-filter schema uid)
                                           (.toString)))

      {:objectclass "person"
       :attributes {}}
      "userUid"
      "(uid=userUid)"

      {:objectclass "user"
       :attributes {:objectclass "class"
                    :uid "id"}}
      "user1"
      "(id=user1)"))


  (testing "user-query"
    (are [directory filter value query] (= query (-> (user-query
                                                       directory
                                                       (filter (get-in directory [:schema :user]) value))
                                                     (update :filter str)))

      {:config {:users-base-dn "dc=amaris,dc=ovh"}
       :schema {:user {:objectclass "person"
                       :attributes {}}}}
      user-uid-filter "userUid"
      {:base-dn "dc=amaris,dc=ovh",
       :attributes [:uid :description :mail :phone :pwdChangedTime],
       :filter "(uid=userUid)"}

      {:config {:users-base-dn "dc=lvmh,dc=com"}
       :schema {:user {:objectclass "user"
                       :attributes {:description "desc" :mail "email"}}}}
      user-mail-filter "user1@lvmh.com"
      {:base-dn "dc=lvmh,dc=com",
       :attributes [:uid :desc :email :phone :pwdChangedTime],
       :filter "(&(objectclass=user)(email=user1@lvmh.com))"}))


  (testing "first-user-found"
    (are [users result] (= result (first-user-found users))

      [{:uid "userUid"}
       {:uid "userUid2"}]
      [{:uid "userUid"} nil]

      []
      [nil ["user not found"]])))
