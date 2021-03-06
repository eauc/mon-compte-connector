(defproject com.amaris.myaccount/connector "_"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojars.pntblnk/clj-ldap "0.0.16"]
                 [org.clojure/tools.cli "0.3.7"]
                 [clj-http "3.9.1"]
                 [clj-time "0.14.4"]
                 [buddy/buddy-auth "2.1.0"]
                 [buddy/buddy-sign "2.2.0"]
                 [cheshire "5.8.0"]
                 [compojure "1.6.1"]
                 [environ "1.1.0"]
                 [expound "0.7.1"]
                 [integrant "0.6.3"]
                 [lock-key "1.5.0"]
                 [one-time "0.4.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-json "0.4.0"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-cloverage "1.0.13"]
            [lein-eftest "0.5.2"]
            [lein-environ "1.1.0"]
            [me.arrdem/lein-git-version "2.0.3"]
            [lein-pprint "1.2.0"]
            [refactor-nrepl "2.4.0-SNAPSHOT"]]
  :profiles {:dev {:env {:dev true}
                   :source-paths ["dev" "test"]
                   :dependencies [[eftest "0.5.3"]
                                  [orchestra "2018.08.19-1"]
                                  [org.clojure/tools.namespace "0.2.11"]]}
             :cloverage {:cloverage {:ns-exclude-regex [#"^.*-dev$"
                                                        #"^.*-test$"
                                                        #"^mon-compte-connector.app$"
                                                        #"^mon-compte-connector.cipher$"
                                                        #"^mon-compte-connector.core$"
                                                        #"^mon-compte-connector.debug$"
                                                        #"^mon-compte-connector.ldap$"
                                                        #"^mon-compte-connector.routes$"
                                                        #"^mon-compte-connector.server$"]}}
             :repl {}
             :uberjar {:main mon-compte-connector.core
                       :aot :all}}
  :repl-options {:init-ns mon-compte-connector.repl}
  :env {:version :project/version}
  :git-version
  {:status-to-version
   (fn [{:keys [tag ref-short ahead? dirty?] :as git}]
     (let [version (if (and tag (not ahead?)) tag ref-short)]
       (if dirty? (str version "-SNAPSHOT") version)))})
