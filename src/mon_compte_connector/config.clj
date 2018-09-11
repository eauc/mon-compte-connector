(ns mon-compte-connector.config
  (:require [cheshire.core :as cs]
            [clojure.java.io :as io]
            [integrant.core :as ig]))


(defn config
  [{:keys [config-file-path certs-file-path certs-file-pwd]}]
  (let [raw-config (-> (io/reader config-file-path)
                       (cs/parse-stream true))]
    (-> raw-config
        (assoc :certs {:certs-file-path certs-file-path
                       :certs-file-pwd certs-file-pwd})
        (assoc-in [:admin :certs] (ig/ref :certs))
        (assoc :routes {:admin (ig/ref :admin)
                        :auth (ig/ref :auth)
                        :directories (ig/ref :directories)})
        (assoc-in [:server :certs] (ig/ref :certs))
        (assoc-in [:server :routes] (ig/ref :routes)))))
