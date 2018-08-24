(ns mon-compte-connector.ldap-directory.filter
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.tools.logging :as log]))


(comment
  (def mail "user1@myDomain.com")

  (def user-schema {:object-class "person"
                    :attributes {:description "description"
                                 :mail "mail"
                                 :phone "mobile"
                                 :password "userPassword"}})
  )



(defn user-mail
  [user-schema mail]
  (Filter/createANDFilter
    [(Filter/createEqualityFilter (name (get-in user-schema [:attributes :objectclass] :objectclass))
                                  (get user-schema :objectclass "person"))
     (Filter/createEqualityFilter (name (get-in user-schema [:attributes :mail] :mail))
                                  mail)]))

(defn user-uid
  [user-schema uid]
  (Filter/createEqualityFilter (name (get-in user-schema [:attributes :uid] :uid)) uid))

(comment
  (-> (user-mail user-schema mail)
      (.toString))
  ;; => (&(objectclass=person)(mail=user1@myDomain.com))

  (-> (user-uid user-schema "userUid")
      (.toString))
  ;; => (uid=userUid)
  )
