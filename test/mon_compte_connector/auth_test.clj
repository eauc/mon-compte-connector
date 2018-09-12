(ns mon-compte-connector.auth-test
  (:import java.util.Date)
  (:require [mon-compte-connector.auth :refer :all]
            [clojure.test :as t :refer [deftest testing is are]]
            [clj-time.core :as time]
            [mon-compte-connector.result :as r]
            [mon-compte-connector.example :refer [example]]))


(deftest auth-test
  (testing "user-claim"
    (let [now (time/date-time 1986 10 14 4 3 27 456)
          user {:uid "userUid"
                :mail "user@mail.com"}
          token (-> user
                    (user-token
                      {:secret "mySecret"
                       :alg :hs256
                       :now now
                       :exp-delay (time/seconds 5)})
                    r/value)]

      (is (= user (:user token)))

      (example

        [test-token options result]

        (= result (user-claim test-token options))

        {:describe "ok"
         :test-token (:token token)
         :options {:secret "mySecret"
                   :alg :hs256
                   :now (time/plus now (time/seconds 1))}
         :result [{:uid "userUid", :mail "user@mail.com" :exp 529646612} nil]}

        {:describe "invalid secred"
         :test-token (:token token)
         :options {:secret "otherSecret"
                   :alg :hs256
                   :now (time/plus now (time/seconds 1))}
         :result [nil ["Message seems corrupt or manipulated."]]}

        {:describe "invalid algo"
         :test-token (:token token)
         :options {:secret "mySecret"
                   :alg :hs512
                   :now (time/plus now (time/seconds 1))}
         :result [nil ["Message seems corrupt or manipulated."]]}

        {:describe "invalid token"
         :test-token nil
         :options {:secret "mySecret"
                   :alg :hs512
                   :now (time/plus now (time/seconds 1))}
         :result [nil ["token is invalid"]]}

        {:describe "token expired"
         :test-token (:token token)
         :options {:secret "mySecret"
                   :alg :hs256
                   :now (time/plus now (time/seconds 6))}
         :result [nil ["Token is expired (529646612)"]]})))


  (testing "user-code"
    (let [options {:time-step 300
                   :date (Date. 0)
                   :store (atom {})
                   :gen-key (constantly "secretKey")}]

      (is (= {:code 342117 :user {:mail "user11@domain1.com"}}
             (r/value (user-code {:mail "user11@domain1.com"} options))))
      (is (= {"user11@domain1.com" "secretKey"}
             (deref (:store options))))

      (is (= {:code 342117 :user {:mail "user11@domain1.com"}}
             (r/value (user-code {:mail "user11@domain1.com"}
                                      (assoc options :date (Date. 290000)))))
          "within same time-step")
      (is (= {"user11@domain1.com" "secretKey"}
             (deref (:store options))))

      (is (= {:code 439975 :user {:mail "user11@domain1.com"}}
             (r/value (user-code {:mail "user11@domain1.com"}
                                      (assoc options :date (Date. 310000)))))
          "within another time-step")))


  (testing "user-code-valid?"
    (let [options {:time-step 300
                   :date (Date. 0)
                   :store (atom {"user11@domain1.com" "secretKey"})
                   :gen-key (constantly "anotherKey")}
          mail "user11@domain1.com"]

      (is (= (r/just mail)
             (user-code-valid? mail 342117 options)))

      (is (= (r/just mail)
             (user-code-valid? mail 342117
                               (assoc options :date (Date. 290000))))
          "before expiration")

      (is (= (r/create nil ["Code is invalid"])
             (user-code-valid? mail 342117
                               (assoc options :date (Date. 310000))))
          "after expiration")

      (is (= (r/create nil ["Code is invalid"])
             (user-code-valid? "user22@domain2.com" 123456 options))
          "another user")

      (is (= (r/create nil ["Code is invalid"])
             (user-code-valid? mail 123456
                               (assoc options :date (Date. 310000))))
          "invalid code")))

  (testing "one-time-claim"
    (let [now (time/date-time 1986 10 14 4 3 27 456)
          options {:secret "mySecret"
                   :exp-delay (time/seconds 10)
                   :alg :hs256
                   :store (atom {})
                   :now now}
          mail "user11@domain1.com"
          {:keys [token]} (r/value (one-time-token mail options))]

      (is (= (r/just {:exp 529646617 :mail mail})
             (one-time-claim mail token options)))

      (is (= (r/create nil ["One-time token is invalid"])
             (one-time-claim mail token
                             (assoc options :now (time/plus now (time/seconds 9)))))
          "token is valid only one time"))

    (let [now (time/date-time 1986 10 14 4 3 27 456)
          options {:secret "mySecret"
                   :exp-delay (time/seconds 10)
                   :alg :hs256
                   :store (atom {})
                   :now now}
          mail "user11@domain1.com"
          {:keys [token]} (r/value (one-time-token mail options))]

      (is (= (r/create nil ["Token is expired (529646617)"])
             (one-time-claim mail token
                             (assoc options :now (time/plus now (time/seconds 11)))))
          "token is expired")

      (is (= (r/create nil ["Message seems corrupt or manipulated."])
             (one-time-claim mail "toto" options))
          "token is invalid")

      (is (= (r/create nil ["Message seems corrupt or manipulated."])
             (one-time-claim mail token (assoc options :alg :hs512)))
          "alg is invalid")

      (is (= (r/create nil ["Message seems corrupt or manipulated."])
             (one-time-claim mail token (assoc options :secret "otherSecret")))
          "secret is invalid")

      (is (= (r/create nil ["One-time token is invalid"])
             (one-time-claim "user22@domain2.com" token options))
          "other user")

      (is (= (r/just {:exp 529646617 :mail mail})
             (one-time-claim mail token options))))))
