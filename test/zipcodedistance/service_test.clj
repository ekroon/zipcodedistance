(ns zipcodedistance.service-test
  (:require [clojure.test :refer :all]
            [zipcodedistance.service :refer :all]))

(deftest zipcode-normalizer
  (testing "Valid zipcodes"
    (is (= [nil "1000AB"] (check-zipcode "1000AB")))
    (is (= [nil "1000AB"] (check-zipcode "1000 AB")))
    (is (= [nil "1000AB"] (check-zipcode "1000ab")))
    (is (= [nil "1000AB"] (check-zipcode "1000 ab"))))
  (testing "Invalid zipcodes"
    (is (= [:invalid-zipcode nil] (check-zipcode "0100AB")))
    (is (= [:invalid-zipcode nil] (check-zipcode "1000  AB")))
    (is (= [:invalid-zipcode nil] (check-zipcode "123CC")))))

(deftest use-zipcode-normalizer
  (with-redefs [post (fn [& params] (future {:status 200 :body (slurp "samples/CorrectResponse")}))]
    (testing "Use normalizer"
      (with-redefs [route-planner-information-request
                    (fn [_ _ from to]
                      (if (not (= ["1000AB" "1234CC"] [from to]))
                        (throw (Exception. (pr-str [from to])))))]
        (do-request "" "" [""] "1000 aB" "1234 cc")))))
