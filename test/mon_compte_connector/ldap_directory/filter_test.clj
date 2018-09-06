(ns mon-compte-connector.ldap-directory.filter-test
  (:require [mon-compte-connector.ldap-directory.filter :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]))

(deftest ldap-directory.filter-test
  (testing "user-mail-filter"
    (example

      [schema mail filter]

      (= filter (-> (user-mail schema mail)
                    (.toString)))

      {:describe "default attributes"
       :schema {:objectclass "person"
                :attributes {}}
       :mail "toto@acme.com"
       :filter "(&(objectclass=person)(mail=toto@acme.com))"}

      {:describe "custom attributes"
       :schema {:objectclass "user"
                :attributes {:objectclass "class"
                             :mail "email"}}
       :mail "user1@acme.com"
       :filter "(&(class=user)(email=user1@acme.com))"}))

  (testing "user-uid-filter"
    (example

      [schema uid filter]

      (= filter (-> (user-uid schema uid)
                    (.toString)))

      {:describe "default attributes"
       :schema {:objectclass "person"
                :attributes {}}
       :uid "userUid"
       :filter "(uid=userUid)"}

      {:describe "custom attributes"
       :schema {:objectclass "user"
                :attributes {:objectclass "class"
                             :uid "id"}}
       :uid "user1"
       :filter "(id=user1)"})))
