(ns mon-compte-connector.event-log
  (:require [mon-compte-connector.result :as result :refer [->result]]))


(defn event-log
  [result {:keys [type domain device-uid]}]
  (cond-> {:status (if (result/ok? result) "OK" "Error")
           :messages (or (result/errors result) [])
           :domain (or domain "N/A")
           :deviceUid (or device-uid "N/A")}
    (not (nil? type)) (assoc :type type)))


(defn notification
  [result {:keys [user-path] :as options :or {user-path []}}]
  (cond-> (event-log result options)
    (result/ok? result) (assoc :passwordExpirationDate
                               (-> result result/value (get-in (conj user-path :pwd-expiration-date))))))


(defn reset-code
  [result? options]
  (cond-> (event-log result? options)
    (result/ok? result?) (-> (assoc :phone (-> result? result/value :user :phone))
                             (assoc :code (-> result? result/value :code)))))
