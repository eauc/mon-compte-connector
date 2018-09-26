(ns mon-compte-connector.ldap-directory
  (:import com.unboundid.ldap.sdk.Filter)
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [lock-key.core :as lk]
            [mon-compte-connector.cipher :as cipher]
            [mon-compte-connector.directory :as dir :refer [Directory DirectoryFilters]]
            [mon-compte-connector.directory-pool :as dir-pool :refer [->DirectoryPool]]
            [mon-compte-connector.ldap :as ldap]
            [mon-compte-connector.ldap-directory.filter :as f]
            [mon-compte-connector.ldap-directory.pwd :as p]
            [mon-compte-connector.ldap-directory.pwd-policy :as pp]
            [mon-compte-connector.ldap-directory.user :as u]
            [mon-compte-connector.result :as r :refer [err->]]
            [cheshire.core :as cs]))


(defn conn
  [directory]
  (-> directory
      :conn
      deref
      r/value))


(defn user-with-pwd-expiration-date
  [user {:keys [config schema lget] :as directory}]
  (let [pwd-policy-schema (get schema :pwd-policy)
        pwd-policy? (err-> (pp/query user config pwd-policy-schema)
                           (lget (conn directory)))]
    (if (and (not (r/ok? pwd-policy?))
             (empty? (r/logs pwd-policy?)))
      (r/create nil ["password policy not found"])
      (err-> pwd-policy?
             (pp/map-attributes pwd-policy-schema)
             (pp/expiration-date user pwd-policy-schema)))))


(def user-not-found "User not found")


(defn user
  [{:keys [schema search] :as directory} filter]
  (let [user-schema (get schema :user)]
    (err-> (r/just (u/query directory filter))
           (search (conn directory) user-not-found)
           (u/map-attributes user-schema)
           (user-with-pwd-expiration-date directory)
           (#(r/just (dissoc % :pwd-policy))))))


(def invalid-credentials "Invalid credentials")


(defn check-user-auth
  [{:keys [dn] :as user} pwd {:keys [bind?] :as directory}]
  (err-> (bind? {:dn dn :pwd pwd} (conn directory))
         (#(if % (r/just user) (r/create nil [invalid-credentials])))))


(defn authenticated-user
  [{:keys [user-mail-filter] :as directory} mail pwd]
  (err-> (user directory (user-mail-filter directory mail))
         (check-user-auth pwd directory)))


(defn user-pwd-reset
  [{:keys [schema modify search user-mail-filter] :as directory} mail new-pwd]
  (let [user-schema (:user schema)]
    (err-> (r/just (u/basic-query directory (user-mail-filter directory mail)))
           (search (conn directory) user-not-found)
           (p/reset-query user-schema new-pwd)
           (modify (conn directory))
           (#(r/just (:post-read %)))
           (u/map-attributes user-schema)
           (user-with-pwd-expiration-date directory))))


(defn user-connection
  [{:keys [dn] :as user} pwd {:keys [bind? get-connection] :as directory}]
  (let [conn? (get-connection (conn directory))
        result? (err-> conn?
                       (#(bind? {:dn dn :pwd pwd} %)))]
    (if-not (r/ok? conn?)
      conn?
      (if-not (and (r/ok? result?) (r/value result?))
        (r/create nil [invalid-credentials])
        (r/just [user (r/value conn?)])))))


(defn pwd-update
  [[user user-conn] {:keys [schema modify release-connection] :as directory} new-pwd]
  (let [user-schema (:user schema)
        result? (err-> (p/reset-query user user-schema new-pwd)
                       (modify user-conn)
                       (#(r/just (:post-read %)))
                       (u/map-attributes user-schema))]
    (release-connection (conn directory) user-conn)
    result?))


(defn user-pwd-update
  [{:keys [schema search user-mail-filter] :as directory} mail pwd new-pwd]
  (let [user-schema (:user schema)]
    (err-> (r/just (u/basic-query directory (user-mail-filter directory mail)))
           (search (conn directory) user-not-found)
           (user-connection pwd directory)
           (pwd-update directory new-pwd)
           (user-with-pwd-expiration-date directory))))


(defn user-update
  [{:keys [schema search modify user-mail-filter] :as directory} mail data]
  (let [user-schema (:user schema)]
    (err-> (r/just (u/basic-query directory (user-mail-filter directory mail)))
           (search (conn directory) user-not-found)
           (u/update-query user-schema data)
           (modify (conn directory))
           (#(r/just (:post-read %)))
           (u/map-attributes user-schema)
           (user-with-pwd-expiration-date directory))))


(defn guard-conn
  [fn {:keys [conn config connect] :as directory} & args]
  (when (not (r/ok? @conn))
    (reset! conn (connect config)))
  (if (r/ok? @conn)
    (apply fn directory args)
    @conn))


(defrecord LDAPDirectory [conn config schema
                          connect get-connection release-connection
                          bind? lget search modify user-mail-filter]
  DirectoryFilters
  (dir/user-mail-filter [this mail]
    (f/user-mail (get-in this [:schema :user]) mail))
  (dir/user-uid-filter [this uid]
    (f/user-uid (get-in this [:schema :user]) uid))
  Directory
  (dir/user [this filter-fn]
    (guard-conn user this (filter-fn this)))
  (dir/authenticated-user [this mail pwd]
    (guard-conn authenticated-user this mail pwd))
  (dir/user-pwd-reset [this mail new-pwd]
    (guard-conn user-pwd-reset this mail new-pwd))
  (dir/user-pwd-update [this mail pwd new-pwd]
    (guard-conn user-pwd-update this mail pwd new-pwd))
  (dir/user-update [this mail data]
    (guard-conn user-update this mail data)))


(defn make-directory
  [{:keys [config schema]}]
  (let [conn (ldap/connect config)]
    (r/create
      (map->LDAPDirectory {:conn (atom conn)
                           :config config
                           :schema schema
                           :connect ldap/connect
                           :get-connection ldap/get-connection
                           :release-connection ldap/release-connection
                           :bind? ldap/bind?
                           :lget ldap/get
                           :search ldap/search
                           :modify ldap/modify
                           :user-mail-filter dir/user-mail-filter})
      (r/logs conn))))


(defn close
  [directory]
  (.close (conn directory))
  (reset! (:conn directory) (r/create nil ["Connection closed"])))


(defn make-directory-pool
  [configs]
  (let [conn-results (map (fn [[k v]] [(name k) (make-directory v)]) configs)]
    (r/create
      (->> conn-results
           (map (fn [[k v]] [k (r/value v)]))
           ->DirectoryPool)
      (->> conn-results
           (map (fn [[k v]] (map #(str k ": " %) (r/logs v))))
           flatten
           (filter (fn [[k v]] (not (nil? v))))))))



(defn decrypt-config
  [{:keys [cipher encrypted] :as raw-config}]
  (if-not encrypted
    raw-config
    ((:decrypt cipher) encrypted)))


(defmethod ig/init-key :directories [_ raw-config]
  (let [{:keys [servers schemas]} (decrypt-config raw-config)
        configs (map (fn [[name {:keys [schema] :as config}]]
                       [name {:config (dissoc config :schema)
                              :schema (get schemas (keyword schema) (get schema :default))}]) servers)
        pool? (make-directory-pool configs)]
    (log/info "Directories init logs..." {:logs (r/logs pool?)})
    (r/value pool?)))


(defmethod ig/halt-key! :directories [_ pool]
  (dir-pool/close pool))
