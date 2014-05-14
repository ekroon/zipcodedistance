(ns zipcodedistance.core
  (:gen-class)
  (:refer-clojure :exclude [read read-string])
  (:use [clojure.tools.reader.edn :only [read read-string]])
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]])
  (:require [zipcodedistance.service :refer [do-request]])
  (:import  [java.io StringWriter]))

;; (defn do-request [_ _ _ from to]
;;   [nil {:distance "50000"}])

(defn handle-csv-line [username password idx line]
  (let [[number from to distance status] line]
    (if (or (not-empty distance) (= 0 idx))
      line
      (let [[err res] (do-request username password
                                  ["https://ws1.webservices.nl/soap_doclit.php"
                                   "https://ws2.webservices.nl/soap_doclit.php"]
                                  from to)]
        (if err
          [number from to "" (or (:faultstring err) err)]
          [number from to (Math/round (/ (read-string (:distance res)) 1000.0)) "OK"])))))

(def cli-options
  ;; An option with a required argument
  [["-i" "--input FILE" "Input file"
    :validate [#(.exists %) "File does not exist"]
    :parse-fn io/as-file]
   ["-o" "--output FILE" "Output file"
    :parse-fn io/as-file]
   ["-u" "--username USERNAME" "Username"]
   ["-P" "--password PASSWORD" "Password"]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Usage: zipcodedistance [options]"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn exit [status msg]
  (binding [*out* *err*]
    (when msg (println msg))
    (System/exit status)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn -main
  "Read zipcode format from stdin and output added distance to stdout"
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        handle (partial handle-csv-line
                        (get options :username)
                        (get options :password))]
    (if errors
      (exit 1 (error-msg errors))
      (let [input-file (:input options)
            output-file (:output options)]
        (if (and input-file output-file)
          (with-open [w (io/writer output-file)
                      r (io/reader input-file)]
            (->> (csv/read-csv r)
                 (map-indexed handle)
                 (#(csv/write-csv w % :newline :cr+lf))))
          (exit 1 summary))
        (exit 0 nil)))))
