(defproject com.amaris.myaccount/connector "1.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojars.pntblnk/clj-ldap "0.0.16"]]
  :main mon-compte-connector.core
  :src ["src" "test"]
  :plugins [[lein-ancient "0.6.15"]
            [lein-pprint "1.2.0"]
            [refactor-nrepl "2.4.0-SNAPSHOT"]]
  :profiles {:dev {}
             :repl {}
             :uberjar {:aot :all}})
