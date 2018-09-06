(ns mon-compte-connector.ldap-directory-test
  (:require [mon-compte-connector.ldap-directory :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]))

(deftest ldap-directory-test
  (testing "first-user-found"
    (example

      [users result]

      (= result (first-user-found users))

      {:describe "found"
       :users [{:uid "userUid"}
               {:uid "userUid2"}]
       :result [{:uid "userUid"} nil]}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      {:describe "not found"
       :users []
       :result [nil ["User not found"]]})))
