(ns mon-compte-connector.ldap-directory.pwd
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.result :refer [->result]]
            [mon-compte-connector.ldap-directory.user :as u]))


(defn reset-query
  [{:keys [dn] :as user} user-schema new-pwd]
  (let [user-pwd-key (-> (get-in user-schema [:attributes :password] "userPassword") keyword)]
    (->result {:dn dn :replace {user-pwd-key new-pwd}
               :post-read (u/read-attributes user-schema)})))
