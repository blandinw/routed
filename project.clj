(defproject routed "0.1.0-SNAPSHOT"
  :description "A simple menubar to easily route the same request to a server or another."
  :url "https://github.com/blandinw/routed"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1978"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]]
  :plugins [[lein-cljsbuild "0.3.4"]]
  :source-paths ["src/clj"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :warnings true
                        :incremental false
                        :compiler {:output-to "public/js/cljs.js"
                                   :optimizations :whitespace}}
                       {:id "prod"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "public/js/cljs.min.js"
                                   :optimizations :advanced}}]}
  :main routed.core)
