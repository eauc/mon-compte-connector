(ns mon-compte-connector.ldap-directory.pwd
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.result :as r]
            [mon-compte-connector.ldap-directory.user :as u]))


(defn reset-query
  [{:keys [dn] :as user} user-schema new-pwd]
  (let [user-pwd-key (-> user-schema (get-in [:attributes :password] "userPassword") keyword)
        ad-password? (get user-schema :ad-password? false)]
    (r/just {:dn dn
             :replace {user-pwd-key (if ad-password? (.getBytes new-pwd "UTF-16LE") new-pwd)}
             :post-read (:attributes (u/read-attributes user-schema))})))
