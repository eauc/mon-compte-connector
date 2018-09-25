(ns mon-compte-connector.config-dev
  (:require [mon-compte-connector.config :refer :all]
            [clojure.spec.alpha :as s]
            [expound.alpha :as x]))


(comment

  (def test-config
    (load {:config-file-path "config.dev.json"
           :certs-file-path "connector.p12"
           :certs-file-pwd "123456"}))

  ;; (set! s/*explain-out* (x/custom-printer {:show-valid-values? true
  ;;                                          :print-specs? false}))
  ;; (alter-var-root #'s/*explain-out* (constantly (x/custom-printer {:show-valid-values? true
  ;;                                                                  :print-specs? false})))

  (x/expound :mcc.config/config test-config)

  (store test-config "mySecret")

  (load-ks)

  )
