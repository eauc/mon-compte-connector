(ns mon-compte-connector.event-log-test
  (:require [mon-compte-connector.event-log :refer :all]
            [clojure.test :as t :refer [deftest testing is are]]
            [mon-compte-connector.result :as result]))


(deftest event-log-test
  (testing "event-log"
    (are [result options log]

        (= log (event-log result options))

      ;; result
      (result/make-result :value ["error1" "error2"])
      ;; options
      {:domain "domain1.com" :device-uid "#deviceUid1"}
      ;; log
      {:status "OK"
       :messages ["error1" "error2"]
       :domain "domain1.com"
       :deviceUid "#deviceUid1"}

      ;; result
      (result/make-result :value ["error1" "error2"])
      ;; options
      {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"}
      ;; log
      {:status "OK"
       :messages ["error1" "error2"]
       :type "refresh"
       :domain "domain1.com"
       :deviceUid "#deviceUid1"}

      ;; result
      (result/make-result nil ["error1" "error2"])
      ;; options
      {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"}
      ;; log
      {:status "Error"
       :messages ["error1" "error2"]
       :type "refresh"
       :domain "domain1.com"
       :deviceUid "#deviceUid1"}))


  (testing "notification"
    (are [result options notif]

        (= notif (notification result options))

      ;; result
      (result/make-result
        ["server2" {:pwd-expiration-date "2018-08-27T06:52:33Z"}]
        ["error1" "error2"])
      ;; options
      {:type "login" :domain "domain1.com" :device-uid "#deviceUid1"}
      ;; notif
      {:status "OK"
       :messages ["error1" "error2"]
       :type "login"
       :domain "domain1.com"
       :deviceUid "#deviceUid1"
       :passwordExpirationDate "2018-08-27T06:52:33Z"}

      ;; result
      (result/make-result nil ["error1" "error2"])
      ;; options
      {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"}
      ;; notif
      {:status "Error"
       :messages ["error1" "error2"]
       :type "refresh"
       :domain "domain1.com"
       :deviceUid "#deviceUid1"}))


  (testing "reset-code"
    (are [result options reset]

        (= reset (reset-code result options))

      ;; result
      (result/make-result
        ["server2" {:user {:phone "+33123456789"} :code "456123"}]
        ["error1" "error2"])
      ;; options
      {:domain "domain1.com" :device-uid "#deviceUid1"}
      ;; reset
      {:status "OK"
       :messages ["error1" "error2"]
       :domain "domain1.com"
       :deviceUid "#deviceUid1"
       :phone "+33123456789"
       :code "456123"}

      ;; result
      (result/make-result nil ["error1" "error2"])
      ;; options
      {:domain "domain1.com" :device-uid "#deviceUid1"}
      ;; reset
      {:status "Error"
       :messages ["error1" "error2"]
       :domain "domain1.com"
       :deviceUid "#deviceUid1"})))
