(ns mon-compte-connector.ldap-directory-test
  (:require [mon-compte-connector.ldap-directory :refer :all]
            [clojure.test :refer [deftest testing is are]]))

(deftest ldap-directory-test
  (testing "first-user-found"
    (are [users result]

        (= result (first-user-found users))

      ;; users
      [{:uid "userUid"}
       {:uid "userUid2"}]
      ;; result
      [{:uid "userUid"} nil]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; users
      []
      ;; result
      [nil ["User not found"]])))
