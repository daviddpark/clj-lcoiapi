(ns clj-lcoiapi.views.api
  (:require [clj-lcoiapi.views.common :as common]
            [noir.response :as response]
            [clojure.contrib.string :as string])
  (:use [noir.core :only [defpage]]
        [monger.core :only [connect! set-db! get-db]]
        [monger.collection :only [insert]]
        [clj-lcoiapi.core :only
         [classify-trials get-trial parse-study-design-all-trials
          stopped-trials-for-turk]]
        [hiccup.core :only [html]]
        )
  (:import [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern])
  )

(defpage "/test" []
  (response/json {:test "success"}))

(defpage [:get "/trials/stopped"] []
  (response/json (classify-trials)))

(defpage [:get "/trials/studydesign"] []
  (response/json (parse-study-design-all-trials)))

(defpage [:post "/trials/classify"] {:keys [id why_stopped stopped_class]}
  (println (str "Inserting new doc " id "\n" why_stopped "\n" stopped_class "\n"))
  (connect!)
  (set-db! (monger.core/get-db "classification"))
  (insert "whystopped"
          {:nctid id, :why_stopped why_stopped, :stopped_class stopped_class})
  (response/redirect "/trials/classify/thanks")
  )

(defpage [:get ["/trials/:id" :id #"NCT\d+"]] {:keys [id]}
  (response/json (get-trial "http://api.lillycoi.com/v1" id)))

(defpage [:get "/trials/stopped.tsv"] []
  (response/content-type
   "text/tab-separated-values"
   (str "ID\tSTATUS\tWHYSTOPPED\tDESCRIPTION\n"
        (apply str (for [trial (stopped-trials-for-turk)]
                     (str (:id trial) "\t"
                          (:overall_status trial) "\t"
                          (:why_stopped trial) "\t"
                          (if (not (nil? (:textblock
                                          (:detailed_description trial))))
                            (string/replace-re
                             #"\s+" " "
                             (:textblock (:detailed_description trial))))
                          "\n"))))))