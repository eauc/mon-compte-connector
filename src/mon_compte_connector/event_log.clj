(ns mon-compte-connector.event-log
  (:require [mon-compte-connector.result :as r]))


(defn event-log
  [result? {:keys [type domain device-uid]}]
  (cond-> {:status (if (r/ok? result?) "OK" "Error")
           :messages (or (r/logs result?) [])
           :domain (or domain "N/A")
           :deviceUid (or device-uid "N/A")}
    (not (nil? type)) (assoc :type type)))


(defn notification
  [result? {:keys [user-path] :as options :or {user-path []}}]
  (cond-> (event-log result? options)
    (r/ok? result?) (assoc :passwordExpirationDate
                           (-> result? r/value (get-in (conj user-path :pwd-expiration-date))))))


(defn reset-code
  [result? options]
  (cond-> (event-log result? options)
    (r/ok? result?) (-> (assoc :phone (-> result? r/value :user :phone))
                        (assoc :code (-> result? r/value :code)))))
