(ns mon-compte-connector.ldap-directory.pwd
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.result :refer [->result]]
            [mon-compte-connector.ldap-directory.user :as u]))



(defn reset-query
  [{:keys [dn] :as user} user-schema new-pwd]
  (let [user-pwd-key (-> (get-in user-schema [:attributes :password] "userPassword") keyword)]
    (->result {:dn dn :replace {user-pwd-key new-pwd}
               :post-read (u/read-attributes user-schema)})))

(comment
  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})

  (reset-query {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"} user-schema "hello")
  ;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :replace {:password "hello"},
  ;;      :post-read (:uid :description :mail :mobile :pwdChangedTime)}
  ;;     nil]

  (reset-query {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"}
                        {:attributes {:password "password"}} "hello")
  ;; => [{:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :replace {:password "hello"},
  ;;      :post-read (:uid :description :mail :phone :pwdChangedTime)}
  ;;     nil]
  
  )
