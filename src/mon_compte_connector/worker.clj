(ns mon-compte-connector.worker)


(def create vector)


(defn just
  [value]
  (create value nil))


(def value first)


(def logs second)


(defn bind
  [prev-result fn & args]
  (let [result (apply fn (value prev-result) args)]
    (create (value result)
            (concat (or (logs prev-result) [])
                    (logs result)))))
