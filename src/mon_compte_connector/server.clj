(ns mon-compte-connector.server
  (:require [buddy.auth.middleware :refer [wrap-authentication]]
            [clojure.pprint :refer [pprint]]
            [integrant.core :as ig]
            [mon-compte-connector.auth :as auth]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring-debug-logging.core :refer [wrap-with-logger]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]))


(defmethod ig/init-key :server [_ {:keys [certs routes] :as config}]
  (let [handler (-> routes
                    (wrap-authentication auth/basic-backend)
                    wrap-json-response
                    wrap-json-params
                    (wrap-defaults api-defaults)
                    wrap-with-logger)
        options (-> config
                    (assoc :join? false)
                    (assoc :keystore (get-in certs [:server :keystore]))
                    (assoc :key-password (get-in certs [:server :keystore-pass]))
                    (assoc :keystore (get-in certs [:server :keystore]))
                    (assoc :key-password (get-in certs [:server :keystore-pass]))
                    (assoc :truststore (get-in certs [:server :keystore]))
                    (assoc :trust-password (get-in certs [:server :keystore-pass]))
                    (update :client-auth (fnil keyword "none")))]
    (println "Jetty server options")
    (pprint options)
    (run-jetty handler options)))


(defmethod ig/halt-key! :server [_ server]
  (.stop server))
