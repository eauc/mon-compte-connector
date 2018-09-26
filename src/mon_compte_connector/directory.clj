(ns mon-compte-connector.directory)


(defprotocol DirectoryFilters
  (user-mail-filter [this mail] "Filter to find an user by its mail")
  (user-uid-filter [this uid] "Filter to find an user by its UID"))

(defprotocol Directory
  (user [this filter-fn] "Get an user matching a filter")
  (authenticated-user [this mail pwd] "Get an user & check its authentication")
  (user-pwd-reset [this mail new-pwd] "Reset an user password")
  (user-pwd-update [this mail pwd new-pwd] "Update an user password")
  (user-update [this mail data] "Update an user profile"))
