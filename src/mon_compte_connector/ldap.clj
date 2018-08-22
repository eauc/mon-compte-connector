(ns mon-compte-connector.ldap
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.tools.logging :as log]
            [clj-ldap.client :as ldap]))



(defn catch-error
  [fn & args]
  (try
    [(apply fn args) nil]
    (catch Exception e
      (log/error e "LDAP request error")
      [nil [(.getMessage e)]])))



(def connect (partial catch-error ldap/connect))

(comment
  (def config {:host {:address "localhost"
                      :port 636}
               :ssl? true
               :connect-timeout 1000
               :timeout 1000
               :bind-dn "cn=admin,dc=amaris,dc=ovh"
               :password "KLD87cvU"})
  (def conn (first (connect config)))
  )



(defn bind?
  [{:keys [dn pwd]} conn]
  (catch-error ldap/bind? conn dn pwd))

(comment
  (bind? {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh" :pwd "Password14"} conn)
  ;; => [true nil]
  )



(defn get
  [{:keys [dn attributes]} conn]
  (catch-error ldap/get conn dn attributes))

(comment
  (get {:dn "cn=passwordDefault,ou=pwpolicies,dc=amaris,dc=ovh"} conn)
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

  (get {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
        :attributes [:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset]} conn)
  ;; => [{:description "This is John Doe's description",
  ;;      :uid "JohnD",
  ;;      :mail "user1@myDomain.com",
  ;;      :pwdChangedTime "20180821084641Z",
  ;;      :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;      :userPassword "{SSHA}sb2T5cm4Xf9YNDLnNouqmDcrLFohWBK4",
  ;;      :mobile "+3312345678"}
  ;;     nil]
  )



(defn search
  [{:keys [base-dn] :as options} conn]
  (catch-error #(or (ldap/search conn base-dn (dissoc options :base-dn)) '())))

(comment
  (search {:base-dn "dc=amaris,dc=ovh"
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

  (search {:base-dn "dc=amaris,dc=ovh"
           :filter (Filter/createANDFilter
                     [(Filter/createEqualityFilter "objectclass" "person")
                      (Filter/createEqualityFilter "mail" "user1@myDomain.com")])
           :attributes [:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset]} conn)
  ;; => [({:description "This is John Doe's description",
  ;;       :uid "JohnD",
  ;;       :mail "user1@myDomain.com",
  ;;       :pwdChangedTime "20180821084641Z",
  ;;       :dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh",
  ;;       :userPassword "{SSHA}sb2T5cm4Xf9YNDLnNouqmDcrLFohWBK4",
  ;;       :mobile "+3312345678"})
  ;;     nil]
  )



(defn modify
  [{:keys [dn] :as options} conn]
  (catch-error ldap/modify conn dn (dissoc options :dn)))

(comment
  (modify {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
           :replace {:userPassword "hello"}} conn)
  ;; => [{:code 0, :name "success"} nil]
  )



(comment
  (def single-conn (.getConnection conn))

  (bind? {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh" :pwd "hello"} single-conn)
  ;; => [true nil]

  (modify {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
           :replace {:userPassword "hello"}
           :post-read #{:uid :description :mail :mobile :userPassword :pwdChangedTime :pwdReset}} single-conn)
  ;; => [nil ["Password fails quality checking policy"]]

  (modify {:dn "cn=John Doe,ou=Management,dc=amaris,dc=ovh"
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
