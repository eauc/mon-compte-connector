(ns mon-compte-connector.services
  (:require [clojure.pprint :refer [pprint]]
            [mon-compte-connector.admin :as adm]
            [mon-compte-connector.auth :as auth]
            [mon-compte-connector.directory :as dir]
            [mon-compte-connector.event-log :as evl]
            [mon-compte-connector.responses :as res]
            [mon-compte-connector.result :as r :refer [err->]]
            [mon-compte-connector.util :as util]))


(defn user-login
  [{:keys [mail pwd app-build-id device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        result? (err-> (if (r/ok? domain?) (r/just pool) domain?)
                       (dir/authenticated-user mail pwd)
                       (auth/user-token auth-options))]
    (-> (if-not (r/ok? domain?) domain? result?)
        (evl/notification {:user-path [:user]
                           :type "login"
                           :domain (or (r/value domain?) "N/A")
                           :device-uid device-uid})
        (#(adm/send-notification admin %
                                 {:app-build-id app-build-id
                                  :app-device-id device-uid})))
    (res/user-token result?)))


(defn claim-mail-filter
  [mail]
  (r/just #(dir/user-mail-filter % mail)))


(defn user-info
  [{:keys [token app-build-id device-uid]} {:keys [admin auth-options pool]}]
  (let [mail? (err-> (auth/user-claim token auth-options)
                     (#(r/just (:mail %))))
        domain? (err-> mail? (util/domain))
        result? (err-> mail?
                       (claim-mail-filter)
                       (#(dir/user pool %)))]
    (-> result?
        (evl/notification {:type "refresh"
                           :domain (or (r/value domain?) "N/A")
                           :device-uid device-uid})
        (#(adm/send-notification admin %
                                 {:app-build-id app-build-id
                                  :app-device-id device-uid})))
    (res/user-info result?)))


(defn reset-code
  [{:keys [mail app-build-id device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        result? (err-> (dir/user pool #(dir/user-mail-filter % mail))
                       (auth/user-code auth-options))]
    (-> (if-not (r/ok? domain?) domain? result?)
        (evl/reset-code {:domain (or (r/value domain?) "N/A")
                         :device-uid device-uid})
        (#(adm/send-reset-code admin %
                               {:app-build-id app-build-id
                                :app-device-id device-uid})))
    (res/user-code result?)))


(defn reset-token
  [{:keys [mail code app-build-id device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        result? (err-> (auth/user-code-valid? mail code (:code auth-options))
                       (auth/one-time-token (:token auth-options)))]
    (-> result?
        (evl/event-log {:type "resetToken"
                        :domain (or (r/value domain?) "N/A")
                        :device-uid device-uid})
        (#(adm/send-log admin %
                        {:app-build-id app-build-id
                         :app-device-id device-uid})))
    (res/user-ott result?)))


(defn reset-pwd
  [{:keys [mail token new-pwd app-build-id device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        claim? (auth/one-time-claim mail token auth-options)
        result? (err-> claim?
                       (#(r/just (:mail %)))
                       (#(dir/user-pwd-reset pool % new-pwd)))]
    (-> (if-not (r/ok? claim?) claim? result?)
        (evl/notification {:type "passwordReset"
                           :domain (or (r/value domain?) "N/A")
                        :device-uid device-uid})
        (#(adm/send-notification admin %
                                 {:app-build-id app-build-id
                                  :app-device-id device-uid})))
    (res/user-reset-pwd claim? result?)))


(defn change-pwd
  [{:keys [token pwd new-pwd app-build-id device-uid]} {:keys [admin auth-options pool]}]
  (let [mail? (err-> (auth/user-claim token auth-options)
                     (#(r/just (:mail %))))
        domain? (err-> mail? (util/domain))
        result? (err-> mail? (#(dir/user-pwd-update pool % pwd new-pwd)))]
    (-> result?
        (evl/notification {:type "passwordChange"
                           :domain (or (r/value domain?) "N/A")
                           :device-uid device-uid})
        (#(adm/send-notification admin %
                                 {:app-build-id app-build-id
                                  :app-device-id device-uid})))
    (res/user-info result?)))
