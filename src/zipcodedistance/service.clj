(ns zipcodedistance.service
  "Short package description."
  (:require [clojure.data.xml :as xml]
            [org.httpkit.client :as http])
  (:import [org.httpkit.client TimeoutException]))

(def namespaces
  {"soapenv" "http://schemas.xmlsoap.org/soap/envelope/"
   "soap"    "http://www.webservices.nl/soap/"})

(defn add-body [options body]
  (assoc options :body body))

(defn add-soap-action [options action]
  (assoc-in options [:headers "SOAPAction"] action))

(def default-options {:timeout 20000
                      :method :post
                      :headers {"Content-Type" "text/xml;charset=UTF-8"}})
(defn sexp->map [s]
  (into {} (map (fn [xx] [(:tag xx) (-> xx :content first)]) s)))

(defn route-planner-information-request [username password from to]
  (xml/sexp-element :soapenv/Envelope nil
                    [[:soapenv/Header nil
                      [:soap/HeaderLogin nil
                       [:soap/username nil username]
                       [:soap/password nil password]]]
                     [:soapenv/Body nil
                      [:soap/routePlannerInformation nil
                       [:soap/postcodefrom nil from]
                       [:soap/postcodeto nil to]
                       [:soap/routetype nil "fastest"]]]]
                    namespaces))

(declare check-response)

(defn do-request [username password urls from to]
  (loop [[url tail] urls]
    (if url
      (let [options   (-> default-options
                          (add-soap-action "https://ws1.webservices.nl/soap_doclit.php/routePlannerInformation"))
            body      (xml/emit-str (route-planner-information-request username password from to))
            response  @(->> (add-body options body)
                            (http/post url))
            [err res] (check-response response)]
        (if (and err (instance? TimeoutException err))
          (recur tail)
          [err res]))
      [:fallback-exhausted nil])))

(defn get-result [element]
  (-> element
      :content
      first :content
      first :content
      first :content
      sexp->map))

(defn get-error [element]
  (-> element
      :content
      first :content
      first :content
      sexp->map))

(defn check-response [response]
  (if (:body response)
    (let [parsed (xml/parse-str (:body response))]
      (if (= 200 (:status response))
        [nil (get-result parsed)]
        [(get-error parsed) nil]))
    [(or (:error response) :unknown-error) nil]))

;; (def result {:status 200
;;              :body "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">
;;    <SOAP-ENV:Body>
;;       <routePlannerInformationResponse xmlns=\"http://www.webservices.nl/soap/\">
;;          <route>
;;             <time>152</time>
;;             <distance>1333</distance>
;;          </route>
;;       </routePlannerInformationResponse>
;;    </SOAP-ENV:Body>
;; </SOAP-ENV:Envelope>"})

;; (def error {:status 500
;;             :body "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">
;;    <SOAP-ENV:Body>
;;       <SOAP-ENV:Fault>
;;          <faultcode>SOAP-ENV:Client</faultcode>
;;          <faultstring>Incomplete request: missing element</faultstring>
;;          <faultactor/>
;;          <detail/>
;;       </SOAP-ENV:Fault>
;;    </SOAP-ENV:Body>
;; </SOAP-ENV:Envelope>"})
