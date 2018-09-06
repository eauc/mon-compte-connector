(ns mon-compte-connector.auth-test
  (:import java.util.Date)
  (:require [mon-compte-connector.auth :refer :all]
            [clojure.test :as t :refer [deftest testing is are]]
            [clj-time.core :as time]
            [mon-compte-connector.result :as result :refer [->errors ->result]]
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
                    result/value)]

      (is (= user (:user token)))

      (example

        [options result]

        (= result (user-claim (:token token) options))

        {:describe "ok"
         :options {:secret "mySecret"
                   :alg :hs256
                   :now (time/plus now (time/seconds 1))}
         :result [{:uid "userUid", :mail "user@mail.com" :exp 529646612} nil]}

        {:describe "invalid secred"
         :options {:secret "otherSecret"
                   :alg :hs256
                   :now (time/plus now (time/seconds 1))}
         :result [nil ["Message seems corrupt or manipulated."]]}

        {:describe "invalid algo"
         :options {:secret "mySecret"
                   :alg :hs512
                   :now (time/plus now (time/seconds 1))}
         :result [nil ["Message seems corrupt or manipulated."]]}

        {:describe "token expired"
         :options {:secret "mySecret"
                   :alg :hs256
                   :now (time/plus now (time/seconds 6))}
         :result [nil ["Token is expired (529646612)"]]})))


  (testing "user-code"
    (let [options {:time-step 300
                   :date (Date. 0)
                   :store (atom {})
                   :gen-key (constantly "secretKey")}]

      (is (= 342117
             (result/value (user-code {:mail "user11@domain1.com"} options))))
      (is (= {"user11@domain1.com" "secretKey"}
             (deref (:store options))))

      (is (= 342117
             (result/value (user-code {:mail "user11@domain1.com"}
                                      (assoc options :date (Date. 290000)))))
          "within same time-step")
      (is (= {"user11@domain1.com" "secretKey"}
             (deref (:store options))))

      (is (= 439975
             (result/value (user-code {:mail "user11@domain1.com"}
                                      (assoc options :date (Date. 310000)))))
          "within another time-step")))


  (testing "user-code-valid?"
    (let [options {:time-step 300
                   :date (Date. 0)
                   :store (atom {"user11@domain1.com" "secretKey"})
                   :gen-key (constantly "anotherKey")}
          user {:mail "user11@domain1.com"}]

      (is (= (->result user)
             (user-code-valid? user 342117 options)))

      (is (= (->result user)
             (user-code-valid? user 342117
                               (assoc options :date (Date. 290000))))
          "before expiration")

      (is (= (result/make-result nil ["Code is invalid"])
             (user-code-valid? user 342117
                               (assoc options :date (Date. 310000))))
          "after expiration")

      (is (= (result/make-result nil ["Code is invalid"])
             (user-code-valid? (assoc user :mail "user22@domain2.com") 123456 options))
          "another user")

      (is (= (result/make-result nil ["Code is invalid"])
             (user-code-valid? user 123456
                               (assoc options :date (Date. 310000))))
          "invalid code")))

  (testing "onte-time-claim"
    (let [now (time/date-time 1986 10 14 4 3 27 456)
          options {:secret "mySecret"
                   :exp-delay (time/seconds 10)
                   :alg :hs256
                   :store (atom {})
                   :now now}
          user {:mail "user11@domain1.com"}
          {:keys [token]} (result/value (one-time-token user options))]

      (is (= (->result user)
             (one-time-claim user token options)))

      (is (= (->errors ["One-time token is invalid"])
             (one-time-claim user token
                             (assoc options :now (time/plus now (time/seconds 9)))))
          "token is valid only one time"))

    (let [now (time/date-time 1986 10 14 4 3 27 456)
          options {:secret "mySecret"
                   :exp-delay (time/seconds 10)
                   :alg :hs256
                   :store (atom {})
                   :now now}
          user {:mail "user11@domain1.com"}
          {:keys [token]} (result/value (one-time-token user options))]

      (is (= (->errors ["Token is expired (529646617)"])
             (one-time-claim user token
                             (assoc options :now (time/plus now (time/seconds 11)))))
          "token is expired")

      (is (= (->errors ["Message seems corrupt or manipulated."])
             (one-time-claim user "toto" options))
          "token is invalid")

      (is (= (->errors ["Message seems corrupt or manipulated."])
             (one-time-claim user token (assoc options :alg :hs512)))
          "alg is invalid")

      (is (= (->errors ["Message seems corrupt or manipulated."])
             (one-time-claim user token (assoc options :secret "otherSecret")))
          "secret is invalid")

      (is (= (->errors ["One-time token is invalid"])
             (one-time-claim (assoc user :mail "user22@domain2.com") token options))
          "other user")

      (is (= (->result user)
             (one-time-claim user token options))))))
