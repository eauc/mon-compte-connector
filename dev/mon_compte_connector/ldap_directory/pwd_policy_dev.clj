(ns mon-compte-connector.ldap-directory.pwd-policy-dev
  (:require [mon-compte-connector.ldap-directory.pwd-policy :refer :all]
            [clj-time.format :as timef]))


(comment
  (query {:pwd-policy "cn=userPwdPolicy,dc=org"}
         {:default-pwd-policy "cn=defaultPwdPolicy,dc=org"}
         {:attributes {:pwd-max-age "-pwdMaxAge"}})
  ;; => [{:dn "cn=userPwdPolicy,dc=org", :attributes (:-pwdMaxAge)} nil]

  (query {}
         {:default-pwd-policy "cn=defaultPwdPolicy,dc=org"}
         {:attributes {}})
  ;; => [{:dn "cn=defaultPwdPolicy,dc=org", :attributes (:pwdMaxAge)} nil]

  (query {}
         {}
         {:attributes {:pwd-max-age "-pwdMaxAge"}})
  ;; => [nil ["missing password policy"]]

  (expiration-date {:pwd-max-age "7200"}
                   {:pwd-changed-time "20180821195506Z"}
                   {})
  ;; => [{:pwd-changed-time "2018-08-21T19:55:06Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-21T21:55:06Z"}
  ;;     nil]

  (timef/show-formatters)
  ;;    :date-time-no-ms                        2018-08-22T21:09:32Z
  ;;    :basic-date-time-no-ms                  20180822T210932Z

  )
