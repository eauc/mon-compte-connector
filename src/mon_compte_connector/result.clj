(ns mon-compte-connector.result
  (:require [clojure.pprint :refer [pprint]]))


(def make-result vector)


(defn ->result
  ([value error]
   [value (when (nil? value) [error])])
  ([value]
   [value nil]))


(defn ->errors
  [errors]
  [nil errors])


(defn add-errors
  [[result errors] new-errors]
  [result (concat errors new-errors)])


(defn value
  ([result? default]
   (let [v (first result?)]
     (if (nil? v) default v)))
  ([result?]
   (value result? nil)))


(def errors second)


(def ok? (comp not nil? first))


(defn apply-or-error
  [prev? fn & args]
  (if-not (ok? prev?)
    prev?
    (let [result? (apply fn (value prev?) args)]
      (make-result (value result?)
                   (concat (or (errors prev?) []) (errors result?))))))


(defmacro err->
  [val & fns]
  (let [fns (for [f fns] `(apply-or-error ~@f))]
    `(-> ~val
         ~@fns)))
