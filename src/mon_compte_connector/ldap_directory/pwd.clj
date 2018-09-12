(ns mon-compte-connector.ldap-directory.pwd
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.result :as r]
            [mon-compte-connector.ldap-directory.user :as u]))


(defn reset-query
  [{:keys [dn] :as user} user-schema new-pwd]
  (let [user-pwd-key (-> (get-in user-schema [:attributes :password] "userPassword") keyword)]
    (r/just {:dn dn :replace {user-pwd-key new-pwd}
             :post-read (u/read-attributes user-schema)})))
