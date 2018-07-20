(ns mon-compte-connector.core-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [mon-compte-connector.server :refer [handler]]))

(deftest test-app
  (testing "foo route"
    (let [response (handler (mock/request :get "/foo"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello Foo"))))

  (testing "bar route"
    (let [response (handler (mock/request :get "/bar"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello Bar"))))

  (testing "not-found route"
    (let [response (handler (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
