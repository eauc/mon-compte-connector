(ns mon-compte-connector.auth-test
  (:require [mon-compte-connector.auth :refer :all]
            [clojure.test :as t :refer [deftest testing is are]]
            [mon-compte-connector.result :as result]
            [clj-time.core :as time]))


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

      (are [options result]

          (= result (user-claim (:token token) options))

        ;; options
          {:secret "mySecret"
           :alg :hs256
           :now (time/plus now (time/seconds 1))}
          ;; result
          [{:uid "userUid", :mail "user@mail.com" :exp 529646612} nil]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

          ;; options
          {:secret "otherSecret"
           :alg :hs256
           :now (time/plus now (time/seconds 1))}
          ;; result
          [nil ["Message seems corrupt or manipulated."]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

          ;; options
          {:secret "mySecret"
           :alg :hs512
           :now (time/plus now (time/seconds 1))}
          ;; result
          [nil ["Message seems corrupt or manipulated."]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

          ;; options
          {:secret "mySecret"
           :alg :hs256
           :now (time/plus now (time/seconds 6))}
          ;; result
          [nil ["Token is expired (529646612)"]]
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

          ))))
