(ns mon-compte-connector.routes
  (:require [compojure.core :refer [routes GET]]
            [compojure.route :refer [not-found]]
            [ring.util.response :refer [response]]
            [integrant.core :as ig]))

(defmethod ig/init-key :routes
  [_ {:as config}]
  (routes
    ;; (GET "/token" {:keys [identity] :as request}
    ;;      (let [{:keys [email]} identity
    ;;            {:keys [status errors user-info]} (find-by-email* email directories)]
    ;;        (case status
    ;;          :error (assoc (response {:errors errors}) :status 500)
    ;;          :not-found (assoc (response {:errors errors}) :status 404)
    ;;          :ok (response {:status status :user user-info}))))
    (not-found "Not Found")))
