(ns mon-compte-connector.directory-pool
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.result :as r]
            [mon-compte-connector.directory :as dir :refer [Directory]]))



(defn first-result
  [results]
  (->> results
       (filter (fn [[k val]] (r/ok? val)))
       first))


(defn all-logs
  [results]
  (->> results
       (map (fn [[k val]] (map #(str k ": " %) (r/logs val))))
       flatten))


(defn on-pool
  [{:keys [directories] :as pool} fun & args]
  (let [searches (map (fn [[k dir]] [k (apply fun dir args)]) directories)
        result (first-result searches)
        [name result] result
        logs (all-logs searches)]
    (if result
      (r/create (assoc (r/value result) :server name) logs)
      (r/create nil logs))))


(defrecord DirectoryPool [directories]
  Directory
  (dir/user [pool filter-fn]
    (on-pool pool dir/user filter-fn))
  (dir/authenticated-user [pool mail pwd]
    (on-pool pool dir/authenticated-user mail pwd))
  (dir/user-pwd-reset [pool mail new-pwd]
    (on-pool pool dir/user-pwd-reset mail new-pwd))
  (dir/user-pwd-update [pool mail pwd new-pwd]
    (on-pool pool dir/user-pwd-update mail pwd new-pwd))
  (dir/user-update [pool mail data]
    (on-pool pool dir/user-update mail data)))


(defn close
  [pool]
  (on-pool pool close))
