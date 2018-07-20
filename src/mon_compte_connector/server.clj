(ns mon-compte-connector.server
  (:require [mon-compte-connector.auth :refer [basic-backend]]
            [mon-compte-connector.routes :refer [routes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [integrant.core :as ig]))

(def handler
  (-> routes
      (wrap-authentication basic-backend)
      wrap-json-response
      wrap-json-params
      (wrap-defaults api-defaults)))

(defmethod ig/init-key :server [_ config]
  (run-jetty handler (assoc config :join? false)))

(defmethod ig/halt-key! :server [_ server]
  (.stop server))
