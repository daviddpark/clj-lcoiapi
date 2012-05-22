(ns clj-lcoiapi.core
  (:require [clj-http.client :as client]
            [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train])
  (:use [opennlp.nlp]
        [clojure.data.json :only (read-json json-str)])
  (:gen-class))

(defn get-json-content [url]
  (:body (client/get url)))

(defn construct-trial-url [api-server trial-id]
  (str api-server "/trials/" trial-id))

(defn construct-trialsearch-url [api-server fields search_query]
  (str api-server "/trials/search?fields=" (clojure.string/join \, fields)
       "&query=" search_query))

(defn get-trial-count [api-server]
  (:totalCount (read-json (get-json-content (construct-trialsearch-url
                                          api-server
                                          ["id"] "")))))

(defn construct-stopped-url [api-server fields]
  (construct-trialsearch-url api-server fields
                             (str "show_xprt:Y,xprt:%28+%28Terminated%29+OR%28"
                                  "Suspended%29+OR+%28Withdrawn%29+%29+%5B"
                                  "OVERALL-STATUS%5D,count:"
                                  (get-trial-count api-server))))

(defn get-stopped-trials [api-server fields]
  (:results (read-json (get-json-content
                        (construct-stopped-url api-server fields)))))

(defn get-all-trials [api-server fields]
  (:results (read-json
             (get-json-content (construct-trialsearch-url
                                api-server fields
                                (str "count:" (get-trial-count api-server)))))))

(defn add-stopped-reason [model stopped-category trial]
  (let [category-result (stopped-category (:why_stopped trial))]
    {:why_stopped_classification (merge category-result
                                        (meta category-result))}))

(defn match-study-design-facets [design-facet]
  (match-study-design-facets design-facet)
  )

(defn extract-study-design-facets [design-facet]
  (into {}
        (map vec
             (map rest
                  (re-seq #"(?i)((?:\w+\s*)+):\s+((?:[a-z/]+\s*)+(?:\([^:]+\))*)"
                          design-facet)))))

(defn merge-study-design-facets [trial]
  (if (not (nil? (:study_design trial)))
    (merge {:id (:id trial)} {:study_design_facets (extract-study-design-facets
                                        (:study_design trial))})))

(defn classify-trials []
  (let [stopped-model
        (train/train-document-categorization "training/reasonsstopped.train")
        stopped-category
        (nlp/make-document-categorizer stopped-model)]
    (for [trial
          (get-stopped-trials "http://api.lillycoi.com/v1"
                              ["id" "why_stopped"])]
      (if (nil? (:why_stopped trial))
        (merge trial {:why_stopped_class {:best_category "NODATA"}})
        (merge trial (add-stopped-reason
                      stopped-model stopped-category trial))))))

(defn parse-study-design-all-trials []
  (for [trial (get-all-trials "http://api.lillycoi.com/v1"
                              ["id" "study_design"])]
    (merge-study-design-facets trial)))

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

(defn get-trial [api-server trial-id]
  (let [trial (first (:results (read-json
                     (get-json-content
                     (construct-trial-url api-server trial-id)))))]
    (merge trial (merge-study-design-facets trial))))

(defn random-trial []
  (let [api-server "http://api.lillycoi.com/v1"]
    (get-trial api-server (first (drop (rand-int (get-trial-count api-server)) (map :id (get-all-trials api-server ["id"])))))))

(defn random-stopped-trial []
  (let [api-server "http://api.lillycoi.com/v1"
        stopped-trials (get-stopped-trials api-server ["id"])]       
    (get-trial api-server
               (first (drop (rand-int (count stopped-trials))
                            (map :id stopped-trials))))))

(defn stopped-trials-for-turk []
  (let [api-server "http://api.lillycoi.com/v1"
        stopped-trials
        (get-stopped-trials
         api-server
         ["id" "why_stopped" "overall_status" "detailed_description"])]
    (take-nth 50 stopped-trials)))