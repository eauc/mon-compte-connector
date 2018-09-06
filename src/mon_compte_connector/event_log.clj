(ns mon-compte-connector.event-log
  (:require [mon-compte-connector.result :as result :refer [->result]]))


(comment

  (def success
    [["server2"
      {:uid "Us22",
       :description "This is User22's description",
       :mail "user22@domain2.com",
       :phone "+3387654321",
       :pwd-changed-time "2018-08-26T22:52:33Z",
       :pwd-max-age 28800,
       :pwd-expiration-date "2018-08-27T06:52:33Z"
       :code "123456"}]
     '("server1: User not found"
       "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))")])

  (def failure
    [nil
     '("server1: Password fails quality checking policy"
       "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"
       "server2: User not found")])

  )


(defn event-log
  [result {:keys [type domain device-uid]}]
  (cond-> {:status (if (result/ok? result) "OK" "Error")
           :messages (or (result/errors result) [])
           :domain domain
           :deviceUid device-uid}
    (not (nil? type)) (assoc :type type)))

(comment

  (event-log success {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"})
  ;; => {:status "OK",
  ;;     :messages
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"),
  ;;     :domain "domain1.com",
  ;;     :deviceUid "#deviceUid1"}

  (event-log failure {:domain "domain1.com" :device-uid "#deviceUid1"})
  ;; => {:status "Error",
  ;;     :messages
  ;;     ("server1: Password fails quality checking policy"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"
  ;;      "server2: User not found"),
  ;;     :domain "domain1.com",
  ;;     :deviceUid "#deviceUid1"}

  )


(defn notification
  [result {:keys [user-path] :as options :or {user-path []}}]
  (cond-> (event-log result options)
    (result/ok? result) (assoc :passwordExpirationDate
                               (-> result result/value (get-in (conj user-path :pwd-expiration-date))))))

(comment

  (notification success {:type "login" :domain "domain1.com" :device-uid "#deviceUid1"})
  ;; => {:status "OK",
  ;;     :messages
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"),
  ;;     :type "login",
  ;;     :domain "domain1.com",
  ;;     :deviceUid "#deviceUid1",
  ;;     :passwordExpirationDate "2018-08-27T06:52:33Z"}


  (notification failure {:type "refresh" :domain "domain1.com" :device-uid "#deviceUid1"})
  ;; => {:status "Error",
  ;;     :messages
  ;;     ("server1: Password fails quality checking policy"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"
  ;;      "server2: User not found"),
  ;;     :type "refresh",
  ;;     :domain "domain1.com",
  ;;     :deviceUid "#deviceUid1"}

  )


(defn reset-code
  [result options]
  (cond-> (event-log result options)
    (result/ok? result) (-> (assoc :phone (-> result result/value second :user :phone))
                            (assoc :code (-> result result/value second :code)))))

(comment

  (reset-code success {:domain "domain1.com" :device-uid "#deviceUid1"})
  ;; => {:status "OK",
  ;;     :messages
  ;;     ("server1: User not found"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"),
  ;;     :domain "domain1.com",
  ;;     :deviceUid "#deviceUid1",
  ;;     :phone "+3387654321",
  ;;     :code "123456"}

  (reset-code failure {:domain "domain1.com" :device-uid "#deviceUid1"})
  ;; => {:status "Error",
  ;;     :messages
  ;;     ("server1: Password fails quality checking policy"
  ;;      "bad-server: An error occurred while attempting to connect to server localhost:1636:  IOException(LDAPException(resultCode=91 (connect error), errorMessage='An error occurred while attempting to establish a connection to server localhost/127.0.0.1:1636:  ConnectException(Connection refused (Connection refused)), ldapSDKVersion=4.0.4, revision=27051'))"
  ;;      "server2: User not found"),
  ;;     :domain "domain1.com",
  ;;     :deviceUid "#deviceUid1"}

  )
