(ns mon-compte-connector.example
  (:require [clojure.test :as t :refer [deftest testing is are]]))


(defmacro example
  [bindings test & contexts]
  `(do ~@(map (fn [ctxt]
                `(let [{:keys ~bindings} ~ctxt]
                   (is ~test (get ~ctxt :describe (str "for " ~ctxt))))) contexts)))
