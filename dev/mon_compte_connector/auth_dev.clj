(ns mon-compte-connector.auth-dev
  (:import java.util.Date)
  (:require [mon-compte-connector.auth :refer :all]
            [clj-time.core :as time]
            [mon-compte-connector.result :as r]))


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

  (def test-options
    {:date (Date. 0)
     :store (atom {})
     :time-step 15})

  (user-code {:mail "user11@domain1.com"} test-options)
  ;; => [{:user {:mail "user11@domain1.com"}, :code 607182} nil]

  (def test-code
    (:code
     (result/value
       (user-code {:mail "user11@domain1.com"} test-options))))

  (user-code-valid? {:mail "user11@domain1.com"} test-code test-options)
  ;; => [{:mail "user11@domain1.com"} nil]

  (user-code-valid? {:mail "user11@domain1.com"} test-code
                    (assoc test-options :date (Date. 10000)))
  ;; => [nil ["Code is invalid"]]

  (user-code-valid? {:mail "user11@domain1.com"} 123456 test-options)
  ;; => [nil ["Code is invalid"]]

  (def one-time-tokens
    (atom {}))

  (def test-ott-options
    {:secret "mySecret"
     :exp-delay (time/seconds 10)
     :alg :hs256
     :store one-time-tokens})

  (one-time-token "user11@domain1.com" test-ott-options)
  ;; => [{:token
  ;;      "eyJhbGciOiJIUzI1NiJ9.eyJtYWlsIjoidXNlcjExQGRvbWFpbjEuY29tIiwiZXhwIjoxNTM2MTg0NzMyfQ.X4tZDqfkmeGERFFFumhXM_lm0-1QxSnjRocvP30W9Zg"}
  ;;     nil]

  (def test-ott
    (result/value
      (one-time-token "user11@domain1.com" test-ott-options)))

  (one-time-claim "user11@domain1.com" (:token test-ott)
                  (dissoc test-ott-options :now))
  ;; => ["user11@domain1.com" nil]

  (one-time-claim "user11@domain1.com" (:token test-ott)
                  (dissoc test-ott-options :now))
  ;; => [nil ["One-time token is invalid"]]

  (one-time-claim "user11@domain1.com" (:token test-ott)
                  (dissoc test-ott-options :now))
  ;; => [nil ["Token is expired (1536184740)"]]

  (list test-ott)

  (deref one-time-tokens)

  )
