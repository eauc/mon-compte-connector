(ns mon-compte-connector.core-test
  (:require [clojure.test :as t]
            [mon-compte-connector.core :as sut]))

(t/deftest basic-tests
  (t/testing "it says hello to everyone"
    (t/is (= (with-out-str (sut/-main)) "Hello, World!\n"))))
