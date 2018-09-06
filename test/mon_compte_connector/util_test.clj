(ns mon-compte-connector.util-test
  (:require [mon-compte-connector.util :refer :all]
            [clojure.test :refer [deftest is testing]]
            [mon-compte-connector.example :refer [example]]
            [mon-compte-connector.result :refer [->errors ->result]]))


(deftest util
  (testing "domain"
    (example

      [mail result]

      (= result (domain mail))

      {:describe "default case"
       :mail "user1@domain1.com"
       :result (->result "domain1.com")}

      {:describe "subdomain"
       :mail "user1@sub.domain1.com"
       :result (->result "sub.domain1.com")}

      {:describe "invalid mail"
       :mail "user1"
       :result (->errors ["mail format is invalid"])}

      {:describe "invalid domain"
       :mail "user1@domain1.c"
       :result (->errors ["mail format is invalid"])})))
