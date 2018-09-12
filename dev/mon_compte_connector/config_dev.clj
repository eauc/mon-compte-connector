(ns mon-compte-connector.config-dev
  (:require [mon-compte-connector.config :refer :all]))


(comment

  (def test-config
    (load {:config-file-path "config.dev.json"
           :certs-file-path "connector.p12"
           :certs-file-pwd "123456"}))

  (store test-config "mySecret")

  (load-ks)

  )
