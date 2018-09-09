(defproject com.amaris.myaccount/connector "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojars.pntblnk/clj-ldap "0.0.16"]
                 [clj-http "3.9.1"]
                 [clj-time "0.14.4"]
                 [buddy/buddy-auth "2.1.0"]
                 [buddy/buddy-sign "2.2.0"]
                 [integrant "0.6.3"]
                 [one-time "0.4.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.6.1"]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-eftest "0.5.2"]
            [lein-pprint "1.2.0"]
            [refactor-nrepl "2.4.0-SNAPSHOT"]]
  :profiles {:dev {:source-paths ["dev" "test"]
                   :dependencies [[eftest "0.5.3"]
                                  [expound "0.7.1"]
                                  [orchestra "2018.08.19-1"]
                                  [org.clojure/tools.namespace "0.2.11"]]}
             :repl {}
             :uberjar {:main mon-compte-connector.core
                       :aot :all}}
  :repl-options {:init-ns mon-compte-connector.repl
                 :init (mon-compte-connector.repl/init)})
