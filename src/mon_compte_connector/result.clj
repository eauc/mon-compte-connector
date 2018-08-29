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


(def value first)


(def errors second)


(def ok? (comp not nil? value))


(defn apply-or-error
  [x fn & args]
  (if (ok? x)
    (apply fn (value x) args)
    x))


(defmacro err->
  [val & fns]
  (let [fns (for [f fns] `(apply-or-error ~@f))]
    `(-> [~val nil]
         ~@fns)))
