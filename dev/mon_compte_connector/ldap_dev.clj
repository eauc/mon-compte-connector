(ns mon-compte-connector.ldap-dev
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [mon-compte-connector.ldap :refer :all]))


(comment
  (def config {:host {:address "localhost"
                      :port 636}
               :ssl? true
               :connect-timeout 1000
               :timeout 1000
               :bind-dn "cn=admin,dc=domain1,dc=com"
               :password "ldap1AdminPwd"})

  (def conn (first (connect config)))

  (bind? {:dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com" :pwd "Password11"} conn)
  ;; => [true nil]

  (get {:dn "cn=passwordDefault,ou=pwpolicies,dc=domain1,dc=com"} conn)
  ;; => [{:pwdExpireWarning "60",
  ;;      :objectClass #{"device" "top" "pwdPolicyChecker" "pwdPolicy"},
  ;;      :pwdAttribute "userPassword",
  ;;      :pwdLockout "TRUE",
  ;;      :pwdGraceAuthNLimit "0",
  ;;      :pwdMinLength "6",
  ;;      :pwdInHistory "2",
  ;;      :pwdCheckQuality "1",
  ;;      :pwdMaxFailure "3",
  ;;      :dn "cn=passwordDefault,ou=pwpolicies,dc=amaris,dc=ovh",
  ;;      :pwdAllowUserChange "TRUE",
  ;;      :pwdMustChange "FALSE",
  ;;      :pwdMaxAge "7200",
  ;;      :pwdFailureCountInterval "0",
  ;;      :pwdSafeModify "FALSE",
  ;;      :cn "passwordDefault",
  ;;      :pwdLockoutDuration "20"}
  ;;     nil]

  (get {:dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com"
        :attributes [:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset]} conn)
  ;; => [{:description "This is John Doe's description",
  ;;      :uid "JohnD",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwdChangedTime "20180821084641Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :userPassword "{SSHA}sb2T5cm4Xf9YNDLnNouqmDcrLFohWBK4",
  ;;      :mobile "+3312345678"}
  ;;     nil]

  (search {:base-dn "dc=domain1,dc=com"
           :filter (Filter/createEqualityFilter "objectclass" "person")
           :attributes [:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset]} conn)
  ;; => [({:description "This is John Doe's description",
  ;;       :uid "JohnD",
  ;;       :mail "user1@myDomain.com",
  ;;       :pwdChangedTime "20180821084641Z",
  ;;       :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;       :userPassword "{SSHA}sb2T5cm4Xf9YNDLnNouqmDcrLFohWBK4",
  ;;       :mobile "+3312345678"}
  ;;      {:description "This is Robert Dupont's description",
  ;;       :uid "DupontR",
  ;;       :mail "user2@myDomain.com",
  ;;       :pwdChangedTime "20180821084341Z",
  ;;       :dn "cn=Indiana Dosanjh,ou=Administrative,dc=amaris,dc=ovh",
  ;;       :userPassword "{SSHA}DT/ScjlgIKvY8ZuD4+xWKj2hI+bnN29X",
  ;;       :mobile "+3387654321"})
  ;;     nil]

  (search {:base-dn "dc=domain1,dc=com"
           :filter (Filter/createANDFilter
                     [(Filter/createEqualityFilter "objectclass" "person")
                      (Filter/createEqualityFilter "mail" "user11@domain1.com")])
           :attributes [:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset]} conn)
  ;; => [({:description "This is John Doe's description",
  ;;       :uid "JohnD",
  ;;       :mail "user1@myDomain.com",
  ;;       :pwdChangedTime "20180821084641Z",
  ;;       :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;       :userPassword "{SSHA}sb2T5cm4Xf9YNDLnNouqmDcrLFohWBK4",
  ;;       :mobile "+3312345678"})
  ;;     nil]

  (modify {:dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com"
           :replace {:userPassword "hello"}} conn)
  ;; => [{:code 0, :name "success"} nil]

  (def single-conn (.getConnection conn))

  (bind? {:dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com" :pwd "hello"} single-conn)
  ;; => [true nil]

  (modify {:dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com"
           :replace {:userPassword "hello"}
           :post-read #{:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset}} single-conn)
  ;; => [nil ["Password fails quality checking policy"]]

  (modify {:dn "cn=User11,ou=Class1,ou=Users,dc=domain1,dc=com"
           :replace {:userPassword "Password11"}
           :post-read #{:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset}} single-conn)
  ;; => [{:code 0,
  ;;      :name "success",
  ;;      :post-read
  ;;      {:description "This is John Doe's description",
  ;;       :uid "JohnD",
  ;;       :mail "user1@myDomain.com",
  ;;       :pwdChangedTime "20180821090447Z",
  ;;       :userPassword "{SSHA}HeLcYKSE14DezP5rjrrQzf5mw8HdW93H",
  ;;       :mobile "+3312345678"}}
  ;;     nil]

  (.releaseAndReAuthenticateConnection conn single-conn)
  ;; => nil

  (.getCurrentAvailableConnections conn)
  ;; => 1

  (.close conn)
  ;; => nil

  )
