(ns mon-compte-connector.directory-pool
  (:require [clojure.tools.logging :as log]
            [mon-compte-connector.error :as error :refer [->result ->errors err->]]
            [mon-compte-connector.directory :as dir :refer [Directory]]))



(defn first-result
  [results]
  (->> results
       (filter (fn [[k val]] (error/ok? val)))
       first))


(defn all-errors
  [results]
  (->> results
       (map (fn [[k val]] (map #(str k ": " %) (error/errors val))))
       flatten))


(defn on-pool
  [{:keys [directories] :as pool} fun & args]
  (let [searches (map (fn [[k dir]] [k (apply fun dir args)]) directories)
        result (first-result searches)
        [name result] result
        errors (all-errors searches)]
    (if result
      (error/make-error [name (error/result result)] errors)
      (->errors errors))))


(defrecord DirectoryPool [directories]
  Directory
  (dir/user [pool filter-fn]
    (on-pool pool dir/user filter-fn))
  (dir/authenticated-user [pool mail pwd]
    (on-pool pool dir/authenticated-user mail pwd))
  (dir/user-pwd-reset [pool mail new-pwd]
    (on-pool pool dir/user-pwd-reset mail new-pwd))
  (dir/user-pwd-update [pool mail pwd new-pwd]
    (on-pool pool dir/user-pwd-update mail pwd new-pwd)))
