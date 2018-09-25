(ns mon-compte-connector.routes
  (:import java.util.Date)
  (:require [clj-time.core :as time]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [routes GET POST PUT]]
            [compojure.route :refer [not-found]]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [mon-compte-connector.services :as svc]
            [ring.util.response :refer [response]]))


(defn auth-header-token
  [headers]
  (let [auth-header (get headers "authorization" "")
        [_ token] (re-matches #"Bearer\s([^\s]+)" auth-header)]
    token))


(defn wrap-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception error
        (log/error error "Error while handling HTTP request")
        {:status 500
         :body {:status "InternalServerError"
                :messages (if (env :dev) [(.getMessage error)] [])}}))))


(defmethod ig/init-key :routes
  [_ {:keys [admin auth directories] :as config}]
  (let [{auth-code :code auth-token :token} auth]
    (wrap-exception
      (routes
        (POST "/v1/auth/token" {:keys [identity headers]}
              (svc/user-login {:mail (:mail identity) :pwd (:pwd identity)
                               :device-uid (get headers "x-myaccountapp-device-id")}
                              {:admin admin :pool directories
                               :auth-options (auth-token)}))
        (GET "/v1/me" {:keys [headers]}
             (svc/user-info {:token (auth-header-token headers)
                             :device-uid (get headers "x-myaccountapp-device-id")}
                            {:admin admin :pool directories
                             :auth-options (auth-token)}))
        (PUT "/v1/me/password" {:keys [headers params]}
             (svc/change-pwd {:token (auth-header-token headers)
                              :pwd (get params "oldPassword")
                              :new-pwd (get params "newPassword")
                              :device-uid (get headers "x-myaccountapp-device-id")}
                             {:admin admin :pool directories
                              :auth-options (auth-token)}))
        (POST "/v1/reset/code" {:keys [headers params]}
              (svc/reset-code {:mail (get params "mail")
                               :device-uid (get headers "x-myaccountapp-device-id")}
                              {:admin admin :pool directories
                               :auth-options (auth-code)}))
        (POST "/v1/reset/token" {:keys [headers params]}
              (svc/reset-token {:mail (get params "mail")
                                :code (get params "code")
                                :device-uid (get headers "x-myaccountapp-device-id")}
                               {:admin admin :pool directories
                                :auth-options {:code (auth-code)
                                               :token (auth-token)}}))
        (POST "/v1/reset/password" {:keys [headers params]}
              (svc/reset-pwd {:mail (get params "mail")
                              :token (get params "OTT")
                              :new-pwd (get params "newPassword")
                              :device-uid (get headers "x-myaccountapp-device-id")}
                             {:admin admin :pool directories
                              :auth-options (auth-token)}))
        (not-found {:status 404
                    :body {:status "NotFound"
                           :messages ["route not found"]}})))))
