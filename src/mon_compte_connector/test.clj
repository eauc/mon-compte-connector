(ns mon-compte-connector.test
  (:import com.unboundid.ldap.sdk.Filter
           com.unboundid.ldap.sdk.LDAPException)
  (:require [clj-ldap.client :as ldap]))

(def config {:host {:address "localhost"
                    :port 636}
             :ssl? true
             :connect-timeout 1000
             :timeout 1000
             :bind-dn "cn=admin,dc=amaris,dc=ovh"
             :password "KLD87cvU"})

(def conn (doto (ldap/connect config)
            (.setConnectionPoolName "test")))
;; => #'mon-compte-connector.core/conn

(def bad-config {:host {:address "localhost"
                        :port 636}
                 :ssl? true
                 :connect-timeout 1000
                 :timeout 1000
                 :bind-dn "cn=toto,dc=amaris,dc=ovh"
                 :password "KLD87cvU"})

(def bad-conn (ldap/connect bad-config))
;; => CompilerException LDAPException(resultCode=49 (invalid credentials), errorMessage='invalid credentials', ldapSDKVersion=4.0.4, revision=27051), compiling:(form-init4640266224027028501.clj:27:15)

(ldap/bind? conn "cn=admin,dc=amaris,dc=ovh" "KLD87cvU")
;; => true

(ldap/bind? conn "cn=John Doe,ou=Management,dc=amaris,dc=ovh" "Password1")
;; => true

(ldap/get conn "cn=John Doe,ou=Management,dc=amaris,dc=ovh")
;; => {:description "This is John Doe's description",
;;     :postalAddress "Management$Sunnyvale",
;;     :initials "Q. P.",
;;     :objectClass
;;     #{"top" "person" "inetOrgPerson" "organizationalPerson"},
;;     :departmentNumber "8852",
;;     :uid "JohnD",
;;     :mail "user1@myDomain.com",
;;     :l "Sunnyvale",
;;     :title "Chief Management Manager",
;;     :homePhone "+1 206 330-2837",
;;     :telephoneNumber "+1 206 990-3975",
;;     :manager "cn=Gaffney Chenault,ou=Human Resources,dc=amaris,dc=ovh",
;;     :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
;;     :roomNumber "9602",
;;     :pager "+1 206 576-3842",
;;     :userPassword "{SSHA}ziWjRGstsUJw9JT2ZWsZQkyDAE/udG66",
;;     :facsimileTelephoneNumber "+1 206 888-8900",
;;     :givenName "Doe",
;;     :ou "Management",
;;     :sn "Doe",
;;     :mobile "+3312345678",
;;     :employeeType "Normal",
;;     :secretary "cn=Edlene Sosa,ou=Payroll,dc=amaris,dc=ovh",
;;     :carLicense "Q6UMNK",
;;     :cn "John Doe"}

(ldap/search conn "dc=amaris,dc=ovh" {:filter (Filter/createEqualityFilter "objectclass" "person")
                                      :attributes [:uid :description :mail :mobile]})
;; => ({:description "This is John Doe's description",
;;      :uid "JohnD",
;;      :mail "user1@myDomain.com",
;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
;;      :mobile "+3312345678"}
;;     {:description "This is Robert Dupont's description",
;;      :uid "DupontR",
;;      :mail "user2@myDomain.com",
;;      :dn "cn=Indiana Dosanjh,ou=Administrative,dc=amaris,dc=ovh",
;;      :mobile "+3387654321"})

(ldap/search conn "dc=amaris,dc=ovh" {:filter (Filter/createANDFilter
                                                [(Filter/createEqualityFilter "objectclass" "person")
                                                 (Filter/createEqualityFilter "mail" "user1@myDomain.com")])
                                      :attributes [:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset]})
;; => ({:description "This is John Doe's description",
;;      :uid "JohnD",
;;      :mail "user1@myDomain.com",
;;      :pwdChangedTime "20180820210412Z",
;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
;;      :userPassword "{SSHA}RFyju44Ndm40hlFMnUuzdU8iXrAwK8qC",
;;      :mobile "+3312345678"})

(-> (Filter/createANDFilter
      [(Filter/createEqualityFilter "objectclass" "person")
       (Filter/createEqualityFilter "mail" "user1@myDomain.com")])
    .toNormalizedString)
;; => (&(objectclass=person)(mail=user1@mydomain.com))

(ldap/get conn "cn=passwordDefault,ou=pwpolicies,dc=amaris,dc=ovh")
;; => {:pwdExpireWarning "60",
;;     :objectClass #{"device" "top" "pwdPolicyChecker" "pwdPolicy"},
;;     :pwdAttribute "userPassword",
;;     :pwdLockout "TRUE",
;;     :pwdGraceAuthNLimit "0",
;;     :pwdMinLength "6",
;;     :pwdInHistory "2",
;;     :pwdCheckQuality "1",
;;     :pwdMaxFailure "3",
;;     :dn "cn=passwordDefault,ou=pwpolicies,dc=amaris,dc=ovh",
;;     :pwdAllowUserChange "TRUE",
;;     :pwdMustChange "FALSE",
;;     :pwdMaxAge "7200",
;;     :pwdFailureCountInterval "0",
;;     :pwdSafeModify "FALSE",
;;     :cn "passwordDefault",
;;     :pwdLockoutDuration "20"}

(ldap/modify conn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
             {:replace {:userPassword "toto"}})
;; => {:code 0, :name "success"}

(def single-conn (.getConnection conn))

(ldap/bind? single-conn "cn=John Doe,ou=Management,dc=amaris,dc=ovh" "toto")
;; => true

(ldap/modify single-conn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
             {:replace {:userPassword "hello"}})
;; => LDAPException Password fails quality checking policy  com.unboundid.ldap.sdk.LDAPConnection.modify (LDAPConnection.java:2881)

(ldap/modify single-conn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
             {:replace {:userPassword "Password14"}
              :post-read #{:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset}})
;; => {:code 0,
;;     :name "success",
;;     :post-read
;;     {:description "This is John Doe's description",
;;      :uid "JohnD",
;;      :mail "user1@myDomain.com",
;;      :pwdChangedTime "20180820220012Z",
;;      :userPassword "{SSHA}SlWeV62gBYtQqWqF7ljnKkBTWB+dgOrx",
;;      :mobile "+3312345678"}}

(.releaseAndReAuthenticateConnection conn single-conn)
;; => nil

(.getCurrentAvailableConnections conn)
;; => 1

(.close conn)
;; => nil
