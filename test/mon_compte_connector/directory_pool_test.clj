(ns mon-compte-connector.directory-pool-test
  (:require [mon-compte-connector.directory-pool :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]
            [mon-compte-connector.result :refer [->result ->errors]]))

(deftest directory-pool-test
  (testing "on-pool"
    (let [test-fn (fn [dir & xs]
                    (case dir
                      "ok" (->result xs)
                      "ko" (->errors xs)
                      (->errors ["not found"])))]

      (example

        [dirs args result]

        (= result (apply on-pool {:directories dirs} test-fn args))

        {:describe "no result"
         :dirs {"server1" "dir1"}
         :args ["arg1"]
         :result [nil ["server1: not found"]]}

        {:describe "result and errors"
         :dirs {"server1" "dir1"
                "server2" "ok"
                "server3" "ko"}
         :args ["arg1" "arg2"]
         :result [["server2" ["arg1" "arg2"]] ["server1: not found"
                                               "server3: arg1"
                                               "server3: arg2"]]}

        {:describe "multiple results -> the first wins"
         :dirs {"server1" "dir1"
                "server2" "ok"
                "server3" "dir1"
                "server4" "ok"}
         :args ["arg1" "arg2"]
         :result [["server2" ["arg1" "arg2"]] ["server1: not found"
                                               "server3: not found"]]}))))
