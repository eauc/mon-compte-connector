(ns mon-compte-connector.core
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer :all]))

(defroutes app-routes
  (GET "/foo" [] "Hello Foo")
  (GET "/bar" [] "Hello Bar")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes api-defaults))

(defn -main []
  (run-jetty app {:port 3000}))
