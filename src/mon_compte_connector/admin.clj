(ns mon-compte-connector.admin
  (:import clojure.lang.ExceptionInfo)
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [mon-compte-connector.result :refer [->errors ->result]]))


(defn send
  [params path {:keys [base-url] :as admin}]
  (try
    (-> (client/post (str base-url "/v1" path)
                     {:form-params params
                      :content-type :json
                      :as :json})
        :body
        ((fn [result]
           (log/info {:notification params
                      :result result} (str "Send " path " success"))
           result))
        ->result)
    (catch ExceptionInfo e
      (log/error e (str "Send " path " error"))
      (->errors [(.getMessage e)]))))


(defprotocol AdminAPI
  (send-log [this log] "Send log")
  (send-notification [this notification] "Send notification create/update request")
  (send-reset-code [this reset-code] "Send reset code request"))


(defrecord admin [base-url]
  AdminAPI
  (send-log [this log] (send "/connectors/log" log this))
  (send-notification [this notification] (send "/notifications" notification this))
  (send-reset-code [this reset-code] (send "/reset/code" reset-code this)))
