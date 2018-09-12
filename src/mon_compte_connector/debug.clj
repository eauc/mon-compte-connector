(ns mon-compte-connector.debug
  (:refer-clojure :exclude [println])
  (:require [clojure.pprint :as pp]
            [environ.core :refer [env]]))


(defn println
  [& args]
  (when (env :debug)
    (apply clojure.core/println args)))


(defn pprint
  [& args]
  (when (env :debug)
    (apply pp/pprint args)))
