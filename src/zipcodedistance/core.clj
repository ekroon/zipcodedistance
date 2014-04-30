(ns zipcodedistance.core
  (:gen-class))

(defn -main
  "Read zipcode format from stdin and output added distance to stdout"
  [& args]
  (doall (map println
            (line-seq (java.io.BufferedReader. *in*)))))
