(ns mon-compte-connector.certs-dev
  (:require [mon-compte-connector.certs :refer :all]))


(comment

  (load {:certs-file-path "connector.p12"
         :certs-file-pwd "123456"} {:on-error identity})

  )
