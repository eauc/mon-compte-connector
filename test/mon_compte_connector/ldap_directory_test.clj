(ns mon-compte-connector.ldap-directory-test
  (:require [mon-compte-connector.ldap-directory :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.error :as error]))

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
      [nil ["user not found"]])))
