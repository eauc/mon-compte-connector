(ns mon-compte-connector.auth
  (:import clojure.lang.ExceptionInfo
           java.util.Date)
  (:require [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [one-time.core :as ot]
            [mon-compte-connector.result :as result :refer [->result ->errors]]))


(defn user-token
  [{:keys [mail uid] :as user} {:keys [now exp-delay secret] :as options}]
  (let [exp (time/plus now exp-delay)
        token (jwt/sign {:mail mail :uid uid :exp exp} secret (dissoc options :secret :now :exp-delay))]
    (->result
      {:user user
       :token token})))


(defn user-claim
  [token {:keys [secret] :as options}]
  (try
    (->result
      (jwt/unsign token secret (dissoc options :secret)))
    (catch ExceptionInfo e
      (->errors [(.getMessage e)]))))


(comment

  (def now (time/now))

  (user-token {:mail "user11@domain1.com" :uid "us11"}
              {:secret "mySecret"
               :now now
               :exp-delay (time/seconds 5)
               :alg :hs256})
  ;; => [{:user {:mail "user11@domain1.com", :uid "us11"},
  ;;      :token
  ;;      "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjExQGRvbWFpbjEuY29tIiwidWlkIjoidXMxMSIsImV4cCI6MTUzNTU0OTg5Nn0.9YFmw58RmVP7JvLGSc9ijXsS2r23Uzt3dnTRDtcKqJI"}
  ;;     nil]

  (def user11-token
    (result/value
      (user-token {:mail "user11@domain1.com" :uid "us11"}
                  {:secret "mySecret"
                   :now now
                   :exp-delay (time/seconds 5)
                   :alg :hs256})))

  (user-claim (:token user11-token) {:secret "mySecret"
                                     :now now
                                     :alg :hs256})
  ;; => [{:mail "user11@domain1.com", :uid "us11", :exp 1535549896} nil]

  (user-claim (:token user11-token) {:secret "mySecret"
                                     :now (time/plus now (time/seconds 6))
                                     :alg :hs256})
  ;; => [nil ["Token is expired (1535549896)"]]

  (user-claim (:token user11-token) {:secret "mySecret"
                                     :now (time/plus now (time/seconds 2))
                                     :alg :hs512})
  ;; => [nil ["Message seems corrupt or manipulated."]]

  )


(def users-keys (atom {}))

(defn user-key
  [{:keys [mail]} {:keys [store gen-key]
                   :or {store users-keys
                        gen-key ot/generate-secret-key}}]
  (or (get @store mail)
      (let [key (gen-key)]
        (swap! store assoc mail key)
        key)))

(defn user-code
  [user {:keys [gen-key store] :as options}]
  (-> user
      (user-key options)
      (ot/get-totp-token (dissoc options :store :gen-key))
      ->result))

(def code-invalid "Code is invalid")

(defn user-code-valid?
  [{:keys [mail] :as user} code {:keys [store] :as options}]
  (let [valid? (ot/is-valid-totp-token? code (user-key user options)
                                        (dissoc options :store :gen-keys))]
    (if valid?
      (->result user)
      (->errors [code-invalid]))))

(comment

  (def test-options
    {:date (Date. 0)
     :time-step 5})

  (user-code {:mail "user11@domain1.com"} test-options)
  ;; => [177403 nil]

  (def test-code
    (result/value
      (user-code {:mail "user11@domain1.com"} test-options)))

  (user-code-valid? {:mail "user11@domain1.com"} test-code test-options)
  ;; => [{:mail "user11@domain1.com"} nil]

  (user-code-valid? {:mail "user11@domain1.com"} test-code
                    (assoc test-options :date (Date. 10000)))
  ;; => [nil ["Code is invalid"]]

  (user-code-valid? {:mail "user11@domain1.com"} 123456 test-options)
  ;; => [nil ["Code is invalid"]]

  )
