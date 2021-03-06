(ns mon-compte-connector.event-log-test
  (:require [mon-compte-connector.event-log :refer :all]
            [clojure.test :as t :refer [deftest testing is are]]
            [mon-compte-connector.example :refer [example]]
            [mon-compte-connector.result :as r]))


(deftest event-log-test
  (testing "event-log"
    (example

      [result options log]

      (= log (event-log result options))

      {:describe "result ok / no type / no errors"
       :result (r/just :value)
       :options {:domain "domain1.com" :device-uid "#deviceUid1"}
       :log {:status "OK"
             :messages []
             :domain "domain1.com"
             :deviceUid "#deviceUid1"}}

      {:describe "result ok / no type"
       :result (r/create :value ["error1" "error2"])
       :options {:domain "domain1.com" :device-uid "#deviceUid1"}
       :log {:status "OK"
             :messages ["error1" "error2"]
             :domain "domain1.com"
             :deviceUid "#deviceUid1"}}

      {:describe "result ok / type"
       :result (r/create :value ["error1" "error2"])
       :options {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"}
       :log {:status "OK"
             :messages ["error1" "error2"]
             :type "refresh"
             :domain "domain1.com"
             :deviceUid "#deviceUid1"}}

      {:describe "error / type"
       :result (r/create nil ["error1" "error2"])
       :options {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"}
       :log {:status "Error"
             :messages ["error1" "error2"]
             :type "refresh"
             :domain "domain1.com"
             :deviceUid "#deviceUid1"}}))


  (testing "notification"
    (example

      [result options notif]

      (= notif (notification result options))

      {:describe "success notification"
       :result (r/create
                 {:pwd-expiration-date "2018-08-27T06:52:33Z"}
                 ["error1" "error2"])
       :options {:type "login" :domain "domain1.com" :device-uid "#deviceUid1"}
       :notif {:status "OK"
               :messages ["error1" "error2"]
               :type "login"
               :domain "domain1.com"
               :deviceUid "#deviceUid1"
               :passwordExpirationDate "2018-08-27T06:52:33Z"}}

      {:describe "success notification / user-path"
       :result (r/create
                 {:user {:pwd-expiration-date "2018-08-27T06:52:33Z"}}
                 ["error1" "error2"])
       :options {:type "login" :domain "domain1.com" :device-uid "#deviceUid1" :user-path [:user]}
       :notif {:status "OK"
               :messages ["error1" "error2"]
               :type "login"
               :domain "domain1.com"
               :deviceUid "#deviceUid1"
               :passwordExpirationDate "2018-08-27T06:52:33Z"}}

      {:describe "error log"
       :result (r/create nil ["error1" "error2"])
       :options {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"}
       :notif {:status "Error"
               :messages ["error1" "error2"]
               :type "refresh"
               :domain "domain1.com"
               :deviceUid "#deviceUid1"}}))


  (testing "reset-code"
    (example

      [result options reset]

      (= reset (reset-code result options))

      {:describe "success -> send reset"
       :result (r/create
                 {:user {:phone "+33123456789"} :code "456123"}
                 ["error1" "error2"])
       :options {:domain "domain1.com" :device-uid "#deviceUid1"}
       :reset {:status "OK"
               :messages ["error1" "error2"]
               :domain "domain1.com"
               :deviceUid "#deviceUid1"
               :phone "+33123456789"
               :code "456123"}}

      {:describe "error log"
       :result (r/create nil ["error1" "error2"])
       :options {:domain "domain1.com" :device-uid "#deviceUid1"}
       :reset {:status "Error"
               :messages ["error1" "error2"]
               :domain "domain1.com"
               :deviceUid "#deviceUid1"}})))
