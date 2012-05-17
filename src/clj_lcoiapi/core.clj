(ns clj-lcoiapi.core
  (:require [clj-http.client :as client]
            [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train])
  (:use [opennlp.nlp]
        [clojure.data.json :only (read-json json-str)])
  (:gen-class))

(defn construct-stopped-url [api-server fields]
  (str api-server "/trials/search?fields=" (clojure.string/join \, fields)
                 "&query=show_xprt:Y,xprt:%28+%28Terminated%29+OR%28Suspended%29+OR+%28Withdrawn%29+%29+%5BOVERALL-STATUS%5D,count:999999"))

(defn get-stopped-trials [api-server fields]
  (:results (read-json (:body (client/get (construct-stopped-url api-server fields))))))

(defn get-why-stopped []
  (map (fn [r]
         ;;(.replaceAll r "\\s+" " ")
         )
       (let [not-nil? (complement nil?)]
         (filter not-nil?
                 (map (fn [t] (:why_stopped t))
                      (get-stopped-trials
                         "http://api.lillycoi.com/v1" ["why_stopped"]))))))

(defn save-all-why-stopped [f]
  (spit f (clojure.string/join "\n" (get-why-stopped))))

(defn add-stopped-reason [model stopped-category trial]
  {:why_stopped_class (stopped-category (:why_stopped trial))}
  )

(defn match-study-design-facets [design-facet]
  (match-study-design-facets design-facet)
  )

(defn extract-study-design-facets [design-facet]
  (into {} (map vec
                (map rest
                     (re-seq #"(?i)((?:\w+\s*)+):\s+((?:[a-z/]+\s*)+(?:\([^:]+\))*)"
                             design-facet)))))

(defn merge-study-design-facets [trial]
  (if (not (nil? (:study_design trial)))
    (merge trial {:study_design_facets (extract-study-design-facets (:study_design trial))})))

(defn classify-trials []
  (let [stopped-model
        (train/train-document-categorization "training/reasonsstopped.train")
        stopped-category
        (nlp/make-document-categorizer stopped-model)]
    (for [trial (get-stopped-trials
                 "http://api.lillycoi.com/v1"
                 ["id" "why_stopped","study_design"])]
      (if (nil? (:why_stopped trial))
        (merge trial {:why_stopped_class "NODATA"})
        (merge trial (add-stopped-reason stopped-model stopped-category trial))))))

(defn classify-trials-and-parse-study-design []
  (for [trial (classify-trials)]
    (merge-study-design-facets trial)))




