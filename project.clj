(def lucene-version "7.5.0")

(defproject org.msync/lucene-clj "0.1.0-SNAPSHOT"
  :description "Lucene bindings for Clojure"
  :url "https://github.com/jaju/lucene-clj"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0-beta4"]
                 [org.apache.lucene/lucene-core ~lucene-version]
                 [org.apache.lucene/lucene-queryparser ~lucene-version]
                 [org.apache.lucene/lucene-analyzers-common ~lucene-version]
                 [org.apache.lucene/lucene-suggest ~lucene-version]]

  :aot :all

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "8" "-source" "8" "-deprecation" "-Xlint:-options"]

  :profiles {:dev  {:dependencies [[org.clojure/data.csv "0.1.4"]]
                    :source-paths ["dev"]
                    :resource-paths ["test-resources"]}

             :test {:dependencies   [[org.clojure/data.csv "0.1.4"]]
                    :resource-paths ["test-resources"]}

             :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]])
