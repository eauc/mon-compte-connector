(ns mon-compte-connector.server
  (:require [mon-compte-connector.routes :refer [routes]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer :all]
            [integrant.core :as ig]))

(def handler
  (-> routes
      (wrap-defaults api-defaults)))

(defmethod ig/init-key :server [_ config]
  (run-jetty handler (assoc config :join? false)))

(defmethod ig/halt-key! :server [_ server]
  (.stop server))
