(ns mon-compte-connector.result)


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
  [x fn & args]
  (if (ok? x)
    (apply fn (value x) args)
    x))


(defmacro err->
  [val & fns]
  (let [fns (for [f fns] `(apply-or-error ~@f))]
    `(-> ~val
         ~@fns)))
