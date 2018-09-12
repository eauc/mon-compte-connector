(ns mon-compte-connector.responses
  (:require [clojure.set :as set]
            [mon-compte-connector.result :as r :refer [err->]]))


(defn bad-request
  [result?]
  {:status 400
   :body {:status "BadRequest"
          :messages (r/logs result?)}})


(defn unauthorized
  [result?]
  {:status 401
   :body {:status "Unauthorized"
          :messages (r/logs result?)}})


(defn internal-error
  [result?]
  {:status 500
   :body {:status "InternalServerError"
          :messages (r/logs result?)}})


(defn user-error-token-expired
  [errors]
  (let [expired? (first (filter #(re-find #"Token is expired" %) errors))]
    (if expired?
      (r/create nil [(unauthorized (r/create nil ["token is expired"]))])
      (r/just errors))))


(defn user-error-invalid
  [errors]
  (let [invalid? (first (filter #(re-find #"is invalid" %) errors))]
    (if invalid?
      (r/create nil [(unauthorized (r/create nil [invalid?]))])
      (r/just errors))))


(defn user-error-pwd-check
  [errors]
  (let [pwd-check? (first (filter #(re-find #"Password" %) errors))
        [_ message] (when pwd-check? (re-matches #".*(Password.*)" pwd-check?))]
    (if pwd-check?
      (r/create nil [(bad-request (r/create nil [message]))])
      (r/just errors))))


(defn user-error-credentials
  [errors]
  (let [credentials? (or (first (filter #(re-find #"Message seems corrupt" %) errors))
                         (first (filter #(re-find #"Invalid credentials" %) errors))
                         (every? #(re-find #"User not found" %) errors))]
    (if credentials?
      (r/create nil [(unauthorized (r/create nil ["invalid credentials"]))])
      (r/just errors))))


(defn user-error-default
  [errors]
  (r/create nil [(internal-error (r/create nil ["internal server error"]))]))


(defn user-error
  [result?]
  (let [errors (r/logs result?)
        response? (err-> (r/just errors)
                         (user-error-token-expired)
                         (user-error-pwd-check)
                         (user-error-invalid)
                         (user-error-credentials)
                         (user-error-default))]
    (first (r/logs response?))))


(defn user
  [user]
  (-> user
      (set/rename-keys {:phone :phoneNumber
                        :pwd-changed-time :passwordChangedTime
                        :pwd-max-age :passwordMaxAge
                        :pwd-expiration-date :passworddExpirationDate})
      (dissoc :uid :dn :server)))


(defn user-token
  [result?]
  (if (r/ok? result?)
    {:status 200
     :body {:status "OK"
            :messages []
            :user (user (:user (r/value result?)))
            :token (:token (r/value result?))}}
    (user-error result?)))


(defn user-info
  [result?]
  (if (r/ok? result?)
    {:status 200
     :body {:status "OK"
            :messages []
            :user (user (r/value result?))}}
    (user-error result?)))


(defn user-code
  [result?]
  (if (r/ok? result?)
    {:status 200
     :body {:status "OK"
            :messages []
            :code (:code (r/value result?))}}
    (user-error result?)))


(defn user-ott
  [result?]
  (if (r/ok? result?)
    {:status 200
     :body {:status "OK"
            :messages []
            :token (:token (r/value result?))}}
    (user-error result?)))


(defn user-reset-pwd
  [claim? result?]
  (if-not (r/ok? claim?)
    (user-error claim?)
    (if-not (r/ok? result?)
      (user-error result?)
      (user-info result?))))
