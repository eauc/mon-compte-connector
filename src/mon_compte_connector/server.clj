(ns mon-compte-connector.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [integrant.core :as ig]))


(defmethod ig/init-key :server [_ {:keys [routes] :as config}]
  (let [handler (-> routes
                    wrap-json-response
                    wrap-json-params
                    (wrap-defaults api-defaults))
        options (-> config
                    (assoc :join? false))]
    (run-jetty handler options)))


(defmethod ig/halt-key! :server [_ server]
  (.stop server))
