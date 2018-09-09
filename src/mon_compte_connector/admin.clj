(ns mon-compte-connector.admin
  (:import clojure.lang.ExceptionInfo)
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [mon-compte-connector.result :refer [->errors ->result]]))


(defn -send
  [params path {:keys [base-url] :as admin}]
  (let [url (str base-url "/v1" path)
        request {:form-params params
                 :content-type :json
                 :as :json}]
    (log/info {:url url :request request} "Sending request to admin")
    (try
      (-> (client/post url request)
          :body
          ((fn [result]
             (log/info {:notification params
                        :result result} (str "Send " path " success"))
             result))
          ->result)
      (catch ExceptionInfo e
        (log/error e (str "Send " path " error"))
        (->errors [(.getMessage e)])))))


(defprotocol AdminAPI
  (send-log [this log] "Send log")
  (send-notification [this notification] "Send notification create/update request")
  (send-reset-code [this reset-code] "Send reset code request"))


(defrecord Admin [base-url]
  AdminAPI
  (send-log [this log] (-send log "/connectors/log" this))
  (send-notification [this notification] (-send notification "/notifications" this))
  (send-reset-code [this reset-code] (-send reset-code "/reset/code" this)))


(defmethod ig/init-key :admin [_ {:keys [base-url] :as config}]
  (Admin. base-url))
