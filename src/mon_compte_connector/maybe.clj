(ns mon-compte-connector.maybe)


(def nothing (constantly nil))


(def just identity)


(def ok? (complement nil?))


(def value identity)


(defn bind
  [prev-result fn & args]
  (when (ok? prev-result)
    (apply fn prev-result args)))
