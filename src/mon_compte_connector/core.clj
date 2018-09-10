(ns mon-compte-connector.core
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as s]
            [mon-compte-connector.app :as app])
  (:gen-class))


(def cli-options
  [[nil "--config CONFIG_FILE" "Configuration file (json)"
    :id :config]
   [nil "--password PASSWORD" "Password for the certificates file"
    :id :password]])


(defn print-usage
  [{:keys [summary]}]
  (println "\n*** Amaris MonCompte LDAP Connector ***")
  (println "Usage: java -jar <connector.jar> [--config CONFIG_FILE] [--password PASSWORD CERTS_FILE]")
  (println "      CERTS_FILE            Certificates files provided by Amaris")
  (println summary))


(defn -main [& args]
  (let [opts (parse-opts args cli-options)
        errors (:errors opts)
        [certs-file-path] (:arguments opts)
        password (get-in opts [:options :password])
        config (get-in opts [:options :config])]
    (cond
      ;; parse-opts errors
      errors (do (println (s/join "\n" errors))
                 (print-usage opts)
                 1)
      ;; certs-file without password
      (and certs-file-path
           (not password)) (do (println "PASSWORD is required with the CERTS_FILE option")
           (print-usage opts)
           1)
      ;; more than one argument
      (> (count (:arguments opts)) 1) (do (println (str "Unrecognized arguments: "
                                                        (s/join (drop 1 (:arguments opts)))))
                                          (print-usage opts)
                                          1)
      ;; ok, start
      :else (app/start {:config-file-path config
                        :certs-file-path certs-file-path
                        :certs-file-pwd password}))))
