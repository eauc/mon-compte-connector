(ns mon-compte-connector.ldap-directory.filter-test
  (:require [mon-compte-connector.ldap-directory.filter :refer :all]
            [clojure.test :refer [deftest testing is are]]
            [mon-compte-connector.error :as error]))

(deftest ldap-directory.filter-test
  (testing "user-mail-filter"
    (are [schema mail filter]

        (= filter (-> (user-mail schema mail)
                      (.toString)))

      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; mail
      "toto@acme.com"
      ;; filter
      "(&(objectclass=person)(mail=toto@acme.com))"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

      ;; schema
      {:objectclass "user"
       :attributes {:objectclass "class"
                    :mail "email"}}
      ;; mail
      "user1@acme.com"
      ;; filter
      "(&(class=user)(email=user1@acme.com))"))

  (testing "user-uid-filter"
    (are [schema uid filter]

        (= filter (-> (user-uid schema uid)
                      (.toString)))

      ;; schema
      {:objectclass "person"
       :attributes {}}
      ;; uid filter
      "userUid" "(uid=userUid)"
;;;;;;;;;;;;;;;;;;;;;

      ;; schema
      {:objectclass "user"
       :attributes {:objectclass "class"
                    :uid "id"}}
      ;; uid filter
      "user1" "(id=user1)")))
