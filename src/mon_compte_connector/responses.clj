(ns mon-compte-connector.responses
  (:require [clojure.set :as set]
            [mon-compte-connector.result :as r :refer [->errors ->result]]))


(defn bad-request
  [result?]
  {:status 400
   :body {:status "BadRequest"
          :messages (r/errors result?)}})


(defn unauthorized
  [result?]
  {:status 401
   :body {:status "Unauthorized"
          :messages (r/errors result?)}})


(defn internal-error
  [result?]
  {:status 500
   :body {:status "InternalServerError"
          :messages (r/errors result?)}})


(defn user-error
  [result?]
  (let [errors (r/errors result?)]
    (cond
      (first (filter #(re-find #"Token is expired" %) errors))
      (unauthorized (->errors ["token is expired"]))

      (first (filter #(re-find #"Message seems corrupt" %) errors))
      (unauthorized (->errors ["invalid credentials"]))

      (first (filter #(re-find #"Invalid credentials" %) errors))
      (unauthorized (->errors ["invalid credentials"]))

      (first (filter #(re-find #"^Password" %) errors))
      (bad-request (->errors [(first (filter #(re-find #"^Password" %) errors))]))

      (every? #(re-find #"User not found" %) errors)
      (unauthorized (->errors ["invalid credentials"]))

      :else (internal-error (->errors ["internal server error"])))))


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
