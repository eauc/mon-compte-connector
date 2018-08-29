(ns mon-compte-connector.auth
  (:import clojure.lang.ExceptionInfo)
  (:require [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
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
