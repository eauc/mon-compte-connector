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
  [params path {:keys [base-url certs headers] :as admin}]
  (let [url (str base-url "/v1" path)
        {:keys [keystore keystore-pass trust-store]} certs
        {:keys [build-id]
         :or {build-id "x-myaccountconnector-build-id"}} headers
        request {:headers {build-id (env :version)}
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
  (send-log [this log] "Send log")
  (send-notification [this notification] "Send notification create/update request")
  (send-reset-code [this reset-code] "Send reset code request"))


(defrecord Admin [base-url certs headers]
  AdminAPI
  (register [this] (-send {} "/connectors/register" this))
  (send-log [this log] (-send log "/connectors/log" this))
  (send-notification [this notification] (-send notification "/notifications" this))
  (send-reset-code [this reset-code] (-send reset-code "/reset/code" this)))


(defmethod ig/init-key :admin [_ {:keys [base-url headers certs] :as config}]
  (when (nil? base-url)
    (throw (ex-info "No AdminAPI URL defined in config" {:admin-config config})))
  (Admin. base-url (:client certs) headers))
