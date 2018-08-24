(ns mon-compte-connector.ldap-directory.pwd-policy
  (:require [clj-time.core :as time]
            [clj-time.format :as timef]
            [clojure.set :refer [map-invert rename-keys]]
            [clojure.tools.logging :as log]
            [mon-compte-connector.error :as error :refer [->result ->errors err->]]))


(defn attributes
  [{:keys [attributes]}]
  (->> attributes
       (map (fn [[k v]] [k (keyword v)]))
       (into {})
       (merge {:pwd-max-age :pwdMaxAge})))

(defn query
  [user config pwd-policy-schema]
  (let [pwd-policy-dn (or (:pwd-policy user) (get config :default-pwd-policy))]
    (->result (when pwd-policy-dn
                {:dn pwd-policy-dn
                 :attributes (vals (attributes pwd-policy-schema))})
              "missing password policy")))

(comment
  (query {:pwd-policy "cn=userPwdPolicy,dc=org"}
         {:default-pwd-policy "cn=defaultPwdPolicy,dc=org"}
         {:attributes {:pwd-max-age "-pwdMaxAge"}})
  ;; => [{:dn "cn=userPwdPolicy,dc=org", :attributes (:-pwdMaxAge)} nil]
  
  (query {}
         {:default-pwd-policy "cn=defaultPwdPolicy,dc=org"}
         {:attributes {}})
  ;; => [{:dn "cn=defaultPwdPolicy,dc=org", :attributes (:pwdMaxAge)} nil]

  (query {}
         {}
         {:attributes {:pwd-max-age "-pwdMaxAge"}})
  ;; => [nil ["missing password policy"]]

  )



(defn map-attributes
  [pwd-policy pwd-policy-schema]
  (->result (rename-keys pwd-policy (map-invert (attributes pwd-policy-schema)))))

(defn format-date-time
  [date-time]
  (timef/unparse (timef/formatters :date-time-no-ms) date-time))

(defn expiration-date
  [{:keys [pwd-max-age] :as pwd-policy}
   {:keys [pwd-changed-time] :as user}
   {:keys [pwd-changed-time-format] :as pwd-policy-schema
    :or {pwd-changed-time-format "yyyyMMddHHmmssZ"}}]
  (try
    (let [formatter (timef/formatter pwd-changed-time-format)
          pwd-max-age (Integer/parseInt pwd-max-age)
          pwd-changed-time (timef/parse formatter pwd-changed-time)
          pwd-expiration-date (time/plus pwd-changed-time (time/seconds pwd-max-age))]
      (->result (assoc user
                       :pwd-max-age pwd-max-age
                       :pwd-expiration-date (format-date-time pwd-expiration-date)
                       :pwd-changed-time (format-date-time pwd-changed-time))))
    (catch Exception e
      (log/error e "Error calculating pwd expiration date")
      (->errors ["invalid pwd expiration date"]))))


(comment
  (expiration-date {:pwd-max-age "7200"}
                   {:pwd-changed-time "20180821195506Z"}
                   {})
  ;; => [{:pwd-changed-time "2018-08-21T19:55:06Z",
  ;;      :pwd-max-age 7200,
  ;;      :pwd-expiration-date "2018-08-21T21:55:06Z"}
  ;;     nil]
  
  (timef/show-formatters)
  ;;    :date-time-no-ms                        2018-08-22T21:09:32Z
  ;;    :basic-date-time-no-ms                  20180822T210932Z

  )
