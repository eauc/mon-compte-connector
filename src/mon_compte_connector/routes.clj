(ns mon-compte-connector.routes
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]))

(defroutes routes
  (GET "/foo" [] "Hello Foo")
  (GET "/bar" [] "Hello Bar")
  (route/not-found "Not Found"))
