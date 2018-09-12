(ns mon-compte-connector.result
  (:require [clojure.pprint :refer [pprint]]
            [mon-compte-connector.maybe :as maybe]
            [mon-compte-connector.worker :as worker]))


(defn create
  [value logs]
  (worker/create (maybe/just value) logs))


(def just (comp worker/just maybe/just))


(def value (comp maybe/value worker/value))


(def logs worker/logs)


(def ok? (comp maybe/ok? worker/value))


(defn bind
  [prev-result fn & args]
  (worker/bind prev-result #(apply maybe/bind % fn args)))


(defmacro err->
  [val & fns]
  (let [fns (for [f fns] `(bind ~@f))]
    `(-> ~val
         ~@fns)))
