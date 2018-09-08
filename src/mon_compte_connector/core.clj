(ns mon-compte-connector.core
  (:require [mon-compte-connector.app :as app])
  (:gen-class))


(defn -main [& args]
  (app/start (first args)))
