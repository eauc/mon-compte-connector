(ns mon-compte-connector.admin
  (:import clojure.lang.ExceptionInfo)
  (:require [clojure.pprint :refer [pprint]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [mon-compte-connector.result :as r]))


(defn -send
  [params path {:keys [base-url certs] :as admin}]
  (let [url (str base-url "/v1" path)
        {:keys [keystore keystore-pass trust-store]} certs
        request {:form-params params
                 :content-type :json
                 :as :json
                 :keystore keystore
                 :keystore-pass keystore-pass
                 :trust-store keystore}]
    (log/info {:url url :request request} "Sending request to admin")
    (pprint {:url url :request request})
    (try
      (-> (client/post url request)
          :body
          ((fn [result]
             (log/info {:notification params
                        :result result} (str "Send " path " success"))
             (pprint {:notification params
                      :result result})
             result))
          r/just)
      (catch Exception e
        (log/error e (str "Send " path " error"))
        (pprint e)
        (r/create nil [(.getMessage e)])))))


(defprotocol AdminAPI
  (send-log [this log] "Send log")
  (send-notification [this notification] "Send notification create/update request")
  (send-reset-code [this reset-code] "Send reset code request"))


(defrecord Admin [base-url certs secret]
  AdminAPI
  (send-log [this log] (-send log "/connectors/log" this))
  (send-notification [this notification] (-send notification "/notifications" this))
  (send-reset-code [this reset-code] (-send reset-code "/reset/code" this)))


(defmethod ig/init-key :admin [_ {:keys [base-url certs] :as config}]
  (when (nil? base-url)
    (println "No AdminAPI URL defined in config")
    (throw (ex-info {:admin-config config} "No AdminAPI URL defined in config")))
  (Admin. base-url (:client certs) "mySecret"))
