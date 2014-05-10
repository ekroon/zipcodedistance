(ns zipcodedistance.core
  (:gen-class)
  (:refer-clojure :exclude [read read-string])
  (:use [clojure.tools.reader.edn :only [read read-string]])
  (:require [clojure.data.csv :as csv]
            [clojure.tools.cli :refer [parse-opts]])
  (:require [zipcodedistance.service :refer [do-request]])
  (:import  [java.io StringWriter]))

(defn handle-csv-line [username password line]
  (let [[[number from to distance status]] (csv/read-csv line)]
    (let [writer (StringWriter.)]
      (if distance
        (csv/write-csv writer
                       [[number from to distance status]]
                       :newline :cr+lf)
        (let [[err res] (do-request username password
                                    ["https://ws1.webservices.nl/soap_doclit.php"
                                     "https://ws2.webservices.nl/soap_doclit.php"]
                                    from to)]
          (csv/write-csv writer
                         (if err
                           [[number from to nil (or (:faultstring err) err)]]
                           [[number from to (Math/round (/ (read-string (:distance res)) 1000.0)) "OK"]])
                         :newline :cr+lf)))
      (print (str writer))
      (flush))))

(def cli-options
  ;; An option with a required argument
  [["-u" "--username USERNAME" "Username"
    :validate [#(not (empty? %)) "Username mag niet leeg zijn"]]
   ["-P" "--password PASSWORD" "Password"
    :validate [#(not (empty? %)) "Password mag niet leeg zijn"]]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  "Read zipcode format from stdin and output added distance to stdout"
  [& args]
  (let [parsed-opts (parse-opts args cli-options)
        handle (partial handle-csv-line
                        (get-in parsed-opts [:options :username])
                        (get-in parsed-opts [:options :password]))]
    (if (:error parsed-opts)
      (binding [*out* *err*]
        (exit 1 (clojure.string/join (:error parsed-opts))))
      (doall (map handle
                  (line-seq (java.io.BufferedReader. *in*)))))))
