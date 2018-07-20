(ns mon-compte-connector.auth
  (:require [buddy.auth.backends :as backends]))

(defn basic-fn
  [request authdata]
  (let [username (:username authdata)
        password (:password authdata)]
    username))

(def basic-backend
  (backends/basic {:realm "MonCompteConnector"
                   :authfn basic-fn}))
