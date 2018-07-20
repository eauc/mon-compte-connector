(ns mon-compte-connector.routes
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]]
            [ring.util.response :refer [response]]))

(defroutes routes
  (GET "/token" {:keys [identity] :as request}
       (response {:coucouc "Hello token"
                  :identity identity}))
  (GET "/foo" [] "Hello Foo")
  (GET "/bar" [] "Hello Bar")
  (not-found "Not Found"))
