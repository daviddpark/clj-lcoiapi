(ns clj-lcoiapi.core
  (:require [clj-http.client :as client]
            [clojure.contrib.string :as string]
            [opennlp.nlp :as nlp]
            [monger.collection :as mc]
            [opennlp.tools.train :as train])
  (:use [opennlp.nlp]
        [monger.core :only [connect! set-db! get-db]]
        [clojure.data.json :only [read-json json-str]])
  (:import [java.io ByteArrayInputStream])
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

(defn add-stopped-reason [model trial]
  (if (nil? (:why_stopped trial))
    (merge trial {:why_stopped_classification {:best-category "NODATA"}})
    (let [stopped-category (nlp/make-document-categorizer model)
          category-result (stopped-category (string/lower-case (:why_stopped trial)))]
      (merge trial {:why_stopped_classification
                    (merge category-result (meta category-result))}))))

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

(defn classify-trials-with-model [stopped-model fields]
  (let []
    (for [trial (get-stopped-trials "http://api.lillycoi.com/v1" fields)]
      (add-stopped-reason stopped-model trial))))

(defn curated-why-stopped [trial-id]
  (connect!)
  (set-db! (get-db "classification"))
  {:curations (for [doc (mc/find-maps "whystopped" {:nctid trial-id})]
                doc)})

(defn whystopped-language-model []
  (connect!)
  (set-db! (get-db "classification"))
  (let [content
        (apply str
              (for [doc (mc/find-maps "whystopped")]
                (string/lower-case (apply str (for [annotation (:annotations doc)]
                  (str (first annotation) "\t" (string/replace-re #"\s+" " " (first (rest annotation))) "\n"))))))]
    (println "CLASSIFICATION TRAINING MODEL:\n" content)
    (train/train-document-categorization (ByteArrayInputStream. (.getBytes content)))))

(defn classify-individual-trial [trial]
  (add-stopped-reason (whystopped-language-model) trial))

(defn classify-trials []
  (classify-trials-with-model (whystopped-language-model) ["id" "why_stopped"]))

(defn prepare-trial-facets []
  (for [trial (classify-trials-with-model (whystopped-language-model)
                ["id" "overall_status" "phase" "study_design" "study_type" "why_stopped"])]
    (into {:id (:id trial) :best-category (:best-category (:why_stopped_classification trial))
           :overall_status (:overall_status trial) :phase (:phase trial)
           :why_stopped (:why_stopped trial) :study_type (:study_type trial)}
          (merge-study-design-facets trial))))

(defn uniq-values [k coll]
  (set (for [m coll] (get m k))))

(defn group-by-values [k coll]
  (let [uv (uniq-values k coll)]
    (for [cat uv]
      {:name cat, :children (filter (fn [collitem] (= cat (k collitem))) coll)})))

(defn trial-facets [keys coll]
  (let [grouped-values (group-by-values (first keys) coll)]
    (if (= (count keys) 1)
      (for [leaf grouped-values]
        {:name (:name leaf)
         ;;:children (:children leaf)
         :size (count (:children leaf))})
      (for [leaf grouped-values]
        {:name (:name leaf)
         :children (trial-facets (rest keys) (:children leaf))}))))

(defn classify-trials-static []
  (classify-trials-with-model (train/train-document-categorization "training/reasonsstopped.train")))

(defn parse-study-design-all-trials []
  (for [trial (take 50000 (get-all-trials "http://api.lillycoi.com/v1"
                              ["id" "study_design"]))]
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
    (merge (curated-why-stopped trial-id)
           (merge (classify-individual-trial trial) (merge-study-design-facets trial)))))

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