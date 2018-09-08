(ns mon-compte-connector.ldap-directory.filter-dev
  (:require [mon-compte-connector.ldap-directory.filter :refer :all]))


(comment
  (def mail "user1@myDomain.com")

  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})

  (-> (user-mail user-schema mail)
      (.toString))
  ;; => (&(objectclass=person)(mail=user1@myDomain.com))

  (-> (user-uid user-schema "userUid")
      (.toString))
  ;; => (uid=userUid)

  )
