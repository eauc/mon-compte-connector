(ns mon-compte-connector.services
  (:require [clojure.pprint :refer [pprint]]
            [mon-compte-connector.admin :as adm]
            [mon-compte-connector.auth :as auth]
            [mon-compte-connector.directory :as dir]
            [mon-compte-connector.event-log :as evl]
            [mon-compte-connector.responses :as res]
            [mon-compte-connector.result :as r :refer [err-> ->result]]
            [mon-compte-connector.util :as util]))


(defn user-login
  [{:keys [mail pwd device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        result? (err-> (if (r/ok? domain?) (->result pool) domain?)
                       (dir/authenticated-user mail pwd)
                       (auth/user-token auth-options))]
    (-> (if-not (r/ok? domain?) domain? result?)
        (evl/notification {:user-path [:user]
                           :type "login"
                           :domain (r/value domain? "N/A")
                           :device-uid device-uid})
        (#(adm/send-notification admin %)))
    (res/user-token result?)))


(defn claim-mail-filter
  [mail]
  (->result
    #(dir/user-mail-filter % mail)))


(defn user-info
  [{:keys [token device-uid]} {:keys [admin auth-options pool]}]
  (let [mail? (err-> (auth/user-claim token auth-options)
                     (#(->result (:mail %))))
        domain? (err-> mail? (util/domain))
        result? (err-> mail?
                       (claim-mail-filter)
                       (#(dir/user pool %)))]
    (-> result?
        (evl/notification {:type "refresh"
                           :domain (r/value domain? "N/A")
                           :device-uid device-uid})
        (#(adm/send-notification admin %)))
    (res/user-info result?)))


(defn reset-code
  [{:keys [mail device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        result? (err-> (dir/user pool #(dir/user-mail-filter % mail))
                       (auth/user-code auth-options))]
    (-> (if-not (r/ok? domain?) domain? result?)
        (evl/reset-code {:domain (r/value domain? "N/A")
                         :device-uid device-uid})
        (#(adm/send-reset-code admin %)))
    (res/user-code result?)))


(defn reset-token
  [{:keys [mail code device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        result? (err-> (auth/user-code-valid? mail code (:code auth-options))
                       (auth/one-time-token (:token auth-options)))]
    (-> result?
        (evl/event-log {:type "resetToken"
                        :domain (r/value domain? "N/A")
                        :device-uid device-uid})
        (#(adm/send-log admin %)))
    (res/user-ott result?)))


(defn reset-pwd
  [{:keys [mail token new-pwd device-uid]} {:keys [admin auth-options pool]}]
  (let [domain? (util/domain mail)
        claim? (auth/one-time-claim mail token auth-options)
        result? (err-> claim?
                       (#(->result (:mail %)))
                       (#(dir/user-pwd-reset pool % new-pwd)))]
    (-> (if-not (r/ok? claim?) claim? result?)
        (evl/notification {:type "passwordReset"
                        :domain (r/value domain? "N/A")
                        :device-uid device-uid})
        (#(adm/send-notification admin %)))
    (res/user-reset-pwd claim? result?)))


(defn change-pwd
  [{:keys [token pwd new-pwd device-uid]} {:keys [admin auth-options pool]}]
  (let [mail? (err-> (auth/user-claim token auth-options)
                     (#(->result (:mail %))))
        domain? (err-> mail? (util/domain))
        result? (err-> mail? (#(dir/user-pwd-update pool % pwd new-pwd)))]
    (-> result?
        (evl/notification {:type "passwordChange"
                           :domain (r/value domain? "N/A")
                           :device-uid device-uid})
        (#(adm/send-notification admin %)))
    (res/user-info result?)))
