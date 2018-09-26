(ns mon-compte-connector.admin
  (:import clojure.lang.ExceptionInfo)
  (:require [clojure.pprint :refer [pprint]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [mon-compte-connector.debug :as dbg]
            [mon-compte-connector.result :as r]))


(defn -send
  [{:keys [params headers]} path {:keys [base-url certs] :as admin}]
  (let [url (str base-url "/v1" path)
        {:keys [keystore keystore-pass trust-store]} certs
        {:keys [build-id app-build-id app-device-id]} (get admin :headers)
        request {:headers {build-id (env :version)
                           app-build-id (:app-build-id headers)
                           app-device-id (:app-device-id headers)}
                 :form-params params
                 :content-type :json
                 :as :json
                 :keystore keystore
                 :keystore-pass keystore-pass
                 :trust-store keystore}]
    (dbg/pprint {:url url :request request})
    (try
      (-> (client/post url request)
          :body
          ((fn [result]
             (dbg/pprint {:notification params
                          :result result})
             result))
          r/just)
      (catch Exception error
        (log/warn (str "Send " path " error") {:message (.getMessage error)})
        (dbg/pprint error)
        (r/create nil [(.getMessage error)])))))


(defprotocol AdminAPI
  (register [this] "Register connector at startup")
  (send-log [this log headers] "Send log")
  (send-notification [this notification headers] "Send notification create/update request")
  (send-reset-code [this reset-code headers] "Send reset code request"))


(defrecord Admin [base-url certs headers]
  AdminAPI
  (register [this] (-send {} "/connectors/register" this))
  (send-log [this log headers]
    (-send {:params log :headers headers}
           "/connectors/log" this))
  (send-notification [this notification headers]
    (-send {:params notification :headers headers}
           "/notifications" this))
  (send-reset-code [this reset-code headers]
    (-send {:params reset-code :headers headers}
           "/reset/code" this)))


(defmethod ig/init-key :admin [_ {:keys [base-url headers certs] :as config}]
  (when (nil? base-url)
    (throw (ex-info "No AdminAPI URL defined in config" {:admin-config config})))
  (Admin. base-url (:client certs)
          (merge {:build-id "x-myaccountconnector-build-id"
                  :app-build-id "x-myaccountapp-build-id"
                  :app-device-id "x-myaccountapp-device-id"} headers)))
