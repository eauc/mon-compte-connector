(ns mon-compte-connector.config
  (:require [cheshire.core :as cs]
            [clojure.java.io :as io]
            [integrant.core :as ig]))


(defn config
  [config-file-path]
  (let [raw-config (-> (io/reader config-file-path)
                       (cs/parse-stream true))]
    (-> raw-config
        (assoc :routes {:admin (ig/ref :admin)
                        :auth (ig/ref :auth)
                        :directories (ig/ref :directories)})
        (assoc-in [:server :routes] (ig/ref :routes)))))
