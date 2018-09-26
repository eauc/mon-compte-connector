(ns mon-compte-connector.services-test
  (:import java.util.Date)
  (:require [mon-compte-connector.services :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.admin :refer [AdminAPI]]
            [mon-compte-connector.directory :refer [Directory]]
            [mon-compte-connector.result :as r]
            [mon-compte-connector.example :refer [example]]
            [clj-time.core :as time]
            [mon-compte-connector.auth :as auth]))


(defrecord MockAdmin [calls responses]
  AdminAPI
  (send-log [this log headers]
    (swap! calls update :send-log (fnil conj []) [log headers])
    (:send-log (:responses this)))
  (send-notification [this log headers]
    (swap! calls update :send-notification (fnil conj []) [log headers])
    (:send-notification (:responses this)))
  (send-reset-code [this log headers]
    (swap! calls update :send-reset-code (fnil conj []) [log headers])
    (:send-reset-code (:responses this))))


(defrecord MockPool [calls responses]
  Directory
  (user [this filter-fn]
    (swap! calls update :user (fnil conj []) [filter-fn])
    (:user (:responses this)))
  (authenticated-user [this mail pwd]
    (swap! calls update :authenticated-user (fnil conj []) [mail pwd])
    (:authenticated-user (:responses this)))
  (user-pwd-reset [this mail new-pwd]
    (swap! calls update :user-pwd-reset (fnil conj []) [mail new-pwd])
    (:user-pwd-reset (:responses this)))
  (user-pwd-update [this mail pwd new-pwd]
    (swap! calls update :user-pwd-update (fnil conj []) [mail pwd new-pwd])
    (:user-pwd-update (:responses this)))
  (user-update [this mail data]
    (swap! calls update :user-update (fnil conj []) [mail data])
    (:user-update (:responses this))))

(def test-user
  {:description "This is User11's description",
   :phone "+3312345678",
   :uid "us11",
   :mail "user1@domain1.com",
   :pwd-changed-time "2018-08-26T12:32:29Z",
   :dn "cn=User11,ou=Class1,ou=Users,dc=amaris,dc=ovh",
   :pwd-max-age 7200,
   :pwd-expiration-date "2018-08-26T14:32:29Z"})

(def test-user-response
  {:description "This is User11's description",
   :mail "user1@domain1.com",
   :phoneNumber "+3312345678",
   :passwordChangedTime "2018-08-26T12:32:29Z",
   :passwordMaxAge 7200,
   :passwordExpirationDate "2018-08-26T14:32:29Z"})

(deftest services-test
  (testing "user-login"
    (testing "success"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:authenticated-user (r/just test-user)})
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 200,
                :body {:status "OK",
                       :messages [],
                       :user test-user-response,
                       :token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJ1aWQiOiJ1czExIiwiZXhwIjo1Mjk2NDY2MTJ9.Dh3-cxBfk9fvo0CqzU2Qzpns97RQJTXIFsJhBUdDwUI"}}
               (user-login {:mail "user1@domain1.com"
                            :pwd "userPass"
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options))
            "invalid domain")

        (is (= [["user1@domain1.com" "userPass"]]
               (:authenticated-user @calls))
            "should authenticate user")

        (is (= [[{:status "OK",
                  :messages [],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "login",
                  :passwordExpirationDate "2018-08-26T14:32:29Z"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should register notification")))

    (testing "invalid domain"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {})
            options {:auth-options {} :admin admin :pool pool}]

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["mail format is invalid"]}}
               (user-login {:mail "user1@domain1."
                            :pwd "userPass"
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options))
            "should rejects request")

        (is (= [[{:status "Error",
                  :messages ["mail format is invalid"],
                  :domain "N/A",
                  :deviceUid "#deviceUid1",
                  :type "login"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log invalid mail format")))

    (testing "invalid domain"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:authenticated-user
                                   (r/create nil ["User not found"
                                                  "connection error"
                                                  "User not found"])})
            options {:auth-options {} :admin admin :pool pool}]

        (is (= {:status 500,
                :body {:status "InternalServerError",
                       :messages ["internal server error"]}}
               (user-login {:mail "user1@domain1.com"
                            :pwd "userPass"
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options))
            "should rejects request")

        (is (= [[{:status "Error",
                  :messages ["User not found" "connection error" "User not found"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "login"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log invalid mail format"))))


  (testing "user-info"
    (testing "success"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/just test-user)})
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJ1aWQiOiJ1czExIiwiZXhwIjo1Mjk2NDY2MTJ9.Dh3-cxBfk9fvo0CqzU2Qzpns97RQJTXIFsJhBUdDwUI"]

        (is (= {:status 200,
                :body {:status "OK",
                       :messages [],
                       :user test-user-response}}
               (user-info {:token token
                           :app-build-id "#appBuildId1"
                           :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "OK",
                  :messages [],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "refresh",
                  :passwordExpirationDate "2018-08-26T14:32:29Z"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should refresh notification")))

    (testing "invalid token"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/just {})})
            options {:auth-options {}
                     :admin admin :pool pool}]

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["invalid credentials"]}}
               (user-info {:token "invalid"
                           :app-build-id "#appBuildId1"
                           :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["Message seems corrupt or manipulated."],
                  :domain "N/A",
                  :deviceUid "#deviceUid1",
                  :type "refresh"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log error")))

    (testing "user not found"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/create nil ["User not found"])})
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJ1aWQiOiJ1czExIiwiZXhwIjo1Mjk2NDY2MTJ9.Dh3-cxBfk9fvo0CqzU2Qzpns97RQJTXIFsJhBUdDwUI"]

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["invalid credentials"]}}
               (user-info {:token token
                           :app-build-id "#appBuildId1"
                           :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["User not found"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "refresh"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log error"))))


  (testing "reset-code"
    (testing "success"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/just test-user)})
            options {:auth-options {:time-step 300
                                    :date (Date. 0)
                                    :store (atom {})
                                    :gen-key (constantly "secretKey")}
                     :admin admin :pool pool}]

        (is (= {:status 200,
                :body {:status "OK",
                       :messages [],
                       :code 342117}}
               (reset-code {:mail "user1@domain1.com"
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "OK",
                  :messages [],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :phone "+3312345678",
                  :code 342117}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-reset-code @calls))
            "sould request to send code")))

    (testing "invalid mail"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/create nil ["User not found"
                                                        "connection error"])})
            options {:auth-options {:time-step 300
                                    :date (Date. 0)
                                    :store (atom {})
                                    :gen-key (constantly "secretKey")}
                     :admin admin :pool pool}]

        (is (= {:status 500,
                :body {:status "InternalServerError",
                       :messages ["internal server error"]}}
               (reset-code {:mail "user1@domain1."
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["mail format is invalid"],
                  :domain "N/A",
                  :deviceUid "#deviceUid1"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-reset-code @calls))
            "should log error")))

    (testing "user not found"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/create nil ["User not found"
                                                        "connection error"])})
            options {:auth-options {:time-step 300
                                    :date (Date. 0)
                                    :store (atom {})
                                    :gen-key (constantly "secretKey")}
                     :admin admin :pool pool}]

        (is (= {:status 500,
                :body {:status "InternalServerError",
                       :messages ["internal server error"]}}
               (reset-code {:mail "user1@domain1.com"
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["User not found"
                             "connection error"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-reset-code @calls))
            "should log error"))))


  (testing "reset-token"
    (testing "success"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/just test-user)})
            options {:auth-options {:code {:time-step 300
                                           :date (Date. 0)
                                           :store (atom {})
                                           :gen-key (constantly "secretKey")}
                                    :token {:now (time/date-time 1986 10 14 4 3 27 456)
                                            :secret "mySecret"
                                            :alg :hs256
                                            :store (atom {})
                                            :exp-delay (time/seconds 5)}}
                     :admin admin :pool pool}]

        (is (= {:status 200,
                :body {:status "OK",
                       :messages [],
                       :token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"}}
               (reset-token {:mail "user1@domain1.com"
                             :code 342117
                             :app-build-id "#appBuildId1"
                             :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "OK",
                  :messages [],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "resetToken"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-log @calls))
            "should log success")))

    (testing "invalid code"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user (r/just test-user)})
            options {:auth-options {:code {:time-step 300
                                           :date (Date. 0)
                                           :store (atom {})
                                           :gen-key (constantly "secretKey")}
                                    :token {:now (time/date-time 1986 10 14 4 3 27 456)
                                            :secret "mySecret"
                                            :alg :hs256
                                            :store (atom {})
                                            :exp-delay (time/seconds 5)}}
                     :admin admin :pool pool}]

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["Code is invalid"]}}
               (reset-token {:mail "user1@domain1.com"
                             :code 123456
                             :app-build-id "#appBuildId1"
                             :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["Code is invalid"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "resetToken"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-log @calls))
            "should log error"))))

  (testing "reset-pwd"
    (testing "success"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-pwd-reset (r/just test-user)})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :store (atom {"user1@domain1.com" token})
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 200,
                :body {:status "OK",
                       :messages [],
                       :user test-user-response}}
               (reset-pwd {:mail "user1@domain1.com"
                           :new-pwd "newPass"
                           :token token
                           :app-build-id "#appBuildId1"
                           :device-uid "#deviceUid1"} options)))

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["One-time token is invalid"]}}
               (reset-pwd {:mail "user1@domain1.com"
                           :new-pwd "newPass"
                           :token token
                           :app-build-id "#appBuildId1"
                           :device-uid "#deviceUid1"} options)))

        (is (= [["user1@domain1.com" "newPass"]]
               (:user-pwd-reset @calls))
            "should try to reset user pwd")

        (is (= [[{:status "OK",
                  :messages [],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "passwordReset"
                  :passwordExpirationDate "2018-08-26T14:32:29Z"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]
                [{:status "Error",
                  :messages ["One-time token is invalid"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "passwordReset"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success")))

    (testing "invalid token"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-pwd-reset (r/just test-user)})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :store (atom {"user1@domain1.com" token})
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["invalid credentials"]}}
               (reset-pwd {:mail "user1@domain1.com"
                           :new-pwd "newPass"
                           :token "invalid"
                           :app-build-id "#appBuildId1"
                           :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["Message seems corrupt or manipulated."],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "passwordReset"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success")))

    (testing "update failure"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-pwd-reset (r/create nil ["connection error"])})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :store (atom {"user1@domain1.com" token})
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 500,
                :body {:status "InternalServerError",
                       :messages ["internal server error"]}}
               (reset-pwd {:mail "user1@domain1.com"
                           :new-pwd "newPass"
                           :token token
                           :app-build-id "#appBuildId1"
                           :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["connection error"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "passwordReset"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success"))))


  (testing "change-pwd"
    (testing "success"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-pwd-update (r/just test-user)})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 200,
                :body {:status "OK",
                       :messages [],
                       :user {:description "This is User11's description",
                              :mail "user1@domain1.com",
                              :phoneNumber "+3312345678",
                              :passwordChangedTime "2018-08-26T12:32:29Z",
                              :passwordMaxAge 7200,
                              :passwordExpirationDate "2018-08-26T14:32:29Z"}}}
               (change-pwd {:old-pwd "oldPass"
                            :new-pwd "newPass"
                            :token token
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "OK",
                  :messages [],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "passwordChange",
                  :passwordExpirationDate "2018-08-26T14:32:29Z"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success")))

    (testing "invalid token"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-pwd-update (r/just test-user)})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["invalid credentials"]}}
               (change-pwd {:old-pwd "oldPass"
                            :new-pwd "newPass"
                            :token "invalid"
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["Message seems corrupt or manipulated."],
                  :domain "N/A",
                  :deviceUid "#deviceUid1",
                  :type "passwordChange"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success")))

    (testing "update failure"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-pwd-update
                                   (r/create nil
                                             ["Password does not pass the quality checks"])})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 400,
                :body {:status "BadRequest",
                       :messages ["Password does not pass the quality checks"]}}
               (change-pwd {:old-pwd "oldPass"
                            :new-pwd "newPass"
                            :token token
                            :app-build-id "#appBuildId1"
                            :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["Password does not pass the quality checks"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "passwordChange"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success"))))


  (testing "update-profile"
    (testing "success"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-update (r/just test-user)})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 200,
                :body {:status "OK",
                       :messages [],
                       :user {:description "This is User11's description",
                              :mail "user1@domain1.com",
                              :phoneNumber "+3312345678",
                              :passwordChangedTime "2018-08-26T12:32:29Z",
                              :passwordMaxAge 7200,
                              :passwordExpirationDate "2018-08-26T14:32:29Z"}}}
               (update-profile {:data {:description "new desc"}
                                :token token
                                :app-build-id "#appBuildId1"
                                :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "OK",
                  :messages [],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "profileUpdate",
                  :passwordExpirationDate "2018-08-26T14:32:29Z"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success")))

    (testing "invalid token"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-update (r/just test-user)})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 401,
                :body {:status "Unauthorized",
                       :messages ["invalid credentials"]}}
               (update-profile {:data {:description "new desc"}
                                :token "invalid"
                                :app-build-id "#appBuildId1"
                                :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["Message seems corrupt or manipulated."],
                  :domain "N/A",
                  :deviceUid "#deviceUid1",
                  :type "profileUpdate"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success")))

    (testing "update failure"
      (let [calls (atom {})
            admin (MockAdmin. calls {})
            pool (MockPool. calls {:user-update
                                   (r/create nil
                                             ["Password does not pass the quality checks"])})
            token "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjFAZG9tYWluMS5jb20iLCJleHAiOjUyOTY0NjYxMn0.FT9p2O7VXyq7PYdq7V0Us9D-_DYiUahEw_kB0nPS0RU"
            options {:auth-options {:now (time/date-time 1986 10 14 4 3 27 456)
                                    :secret "mySecret"
                                    :alg :hs256
                                    :exp-delay (time/seconds 5)}
                     :admin admin :pool pool}]

        (is (= {:status 400,
                :body {:status "BadRequest",
                       :messages ["Password does not pass the quality checks"]}}
               (update-profile {:data {:description "new desc"}
                                :token token
                                :app-build-id "#appBuildId1"
                                :device-uid "#deviceUid1"} options)))

        (is (= [[{:status "Error",
                  :messages ["Password does not pass the quality checks"],
                  :domain "domain1.com",
                  :deviceUid "#deviceUid1",
                  :type "profileUpdate"}
                 {:app-build-id "#appBuildId1"
                  :app-device-id "#deviceUid1"}]]
               (:send-notification @calls))
            "should log success")))))
