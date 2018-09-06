(ns mon-compte-connector.util
  (:require [mon-compte-connector.result :refer [->errors ->result]]))


(def domain-pattern
  #".*@(([a-zA-Z0-9]+\.)+[a-zA-Z]{2,})$")


(defn domain
  [mail]
  (let [[_ domain] (re-matches domain-pattern mail)]
    (if (nil? domain)
      (->errors ["mail format is invalid"])
      (->result domain))))

(comment

  (domain "user11@domain1.c")
  ;; => [nil ["mail format is invalid"]]

  (domain "user11@domain1.com")
  ;; => ["domain1.com" nil]

  (domain "user11@sub.domain1.com")
  ;; => ["sub.domain1.com" nil]

  )
