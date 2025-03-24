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

(def registry
  (merge
   (m/comparator-schemas)
   (m/type-schemas)
   (m/sequence-schemas)
   (m/base-schemas)
   {:file-name file-name?
    :dir-name dir-name?
    :dir-path dir-path?
    :file-path file-path?
    :file file?}))