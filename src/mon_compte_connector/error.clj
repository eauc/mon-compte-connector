(ns mon-compte-connector.error)


(def make-error vector)


(defn ->result
  ([result error]
   [result (when (nil? result) [error])])
  ([result]
   [result nil]))


(defn ->errors
  [errors]
  [nil errors])


(defn add-errors
  [[result errors] new-errors]
  [result (concat errors new-errors)])


(def result first)


(def errors second)


(def ok? (comp not nil? result))


(defn apply-or-error
  [x fn & args]
  (if (ok? x)
    (apply fn (result x) args)
    x))


(defmacro err->
  [val & fns]
  (let [fns (for [f fns] `(apply-or-error ~@f))]
    `(-> [~val nil]
         ~@fns)))
