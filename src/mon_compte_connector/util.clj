(ns mon-compte-connector.util
  (:require [mon-compte-connector.result :as r]))


(def domain-pattern
  #".*@(([a-zA-Z0-9]+\.)+[a-zA-Z]{2,})$")


(defn domain
  [mail]
  (let [[_ domain] (re-matches domain-pattern mail)]
    (if (nil? domain)
      (r/create nil ["mail format is invalid"])
      (r/just domain))))
