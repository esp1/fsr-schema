(ns esp1.fsr.schema
  (:import [java.io File])
  (:require [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [malli.core :as m]
            [malli.generator :as mg]))

(def file-name?
  (m/-simple-schema
   {:type :file-name
    :pred string?
    :type-properties {:gen/gen (gen/let [filename gen/string-alphanumeric
                                         extension (gen/elements [nil
                                                                  "clj" "cljc" "cljs" "edn"
                                                                  "c" "c++" "cpp" "h" "h++" "hpp"
                                                                  "java"
                                                                  "gif" "jpg" "png" "tiff"
                                                                  "html"
                                                                  "md" "txt"])]
                                 (str filename (when extension
                                                 (str "." extension))))}}))

(def dir-name?
  (m/-simple-schema
   {:type :dir-name
    :pred string?
    :type-properties {:gen/gen (gen/one-of [gen/string-alphanumeric
                                            (gen/elements ["." ".."])])}}))

(def dir-path?
  (m/-simple-schema
   {:type :dir-path
    :pred string?
    :type-properties {:gen/gen (gen/fmap #(str/join File/separator %)
                                         (gen/list (mg/generator dir-name?)))}}))

(def file-path?
  (m/-simple-schema
   {:type :file-path
    :pred string?
    :type-properties {:gen/gen (gen/fmap #(str/join File/separator %)
                                         (gen/let [dir-path (mg/generator dir-path?)
                                                   filename (mg/generator file-name?)]
                                           [dir-path filename]))}}))

(def file?
  (m/-simple-schema
   {:type :file
    :pred #(instance? File %)
    :type-properties {:gen/gen (gen/fmap #(File. %)
                                         (mg/generator file-path?))}}))

;; Cache schemas
(def cache-uri?
  (m/-simple-schema
   {:type :cache-uri
    :pred (fn [s] (and (string? s) (not (str/blank? s))))
    :type-properties {:gen/gen (gen/fmap #(str "/" (str/join "/" %))
                                         (gen/vector gen/string-alphanumeric 1 5))}}))

(def cache-key?
  [:map
   [:uri :string]
   [:root-path :string]])

(def cache-entry?
  [:map
   [:uri :string]
   [:root-path :string]
   [:resolved-path :string]
   [:params :map]
   [:timestamp [:int {:min 1}]]
   [:metadata {:optional true} :map]])

(def cache-config?
  [:map
   [:max-entries {:optional true :default 1000} [:int {:min 1}]]
   [:enabled? {:optional true :default true} :boolean]
   [:eviction-policy {:optional true :default :lru} [:enum :lru :fifo :none]]])

(def cache-metrics?
  [:map
   [:hits [:int {:min 0}]]
   [:misses [:int {:min 0}]]
   [:evictions [:int {:min 0}]]
   [:current-size [:int {:min 0}]]])

(defn file-schemas []
  {:file-name file-name?
   :dir-name dir-name?
   :dir-path dir-path?
   :file-path file-path?
   :file file?})

(defn cache-schemas []
  {:cache-uri cache-uri?
   :cache-key cache-key?
   :cache-entry cache-entry?
   :cache-config cache-config?
   :cache-metrics cache-metrics?})
