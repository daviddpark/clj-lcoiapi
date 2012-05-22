(ns clj-lcoiapi.views.classify
  (:require [clj-lcoiapi.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage defpartial]]
        [clj-lcoiapi.core :only [get-trial random-stopped-trial]]
        [hiccup.core :only [html]]
        [hiccup.form-helpers]))

(def trial-class-map (list (list " -- Please select a category --" "")
  (list "Unexpected unacceptable side effects" :AEISSUE)
  (list "More accurately described as completed" :COMPLETED)
  (list "The reason for stopping may be in the description" :DESCRIPTION)
  (list "The trial was stopped due to changes in the study design" :DESIGNCHANGE)
  (list "The trial was stopped due to external factors" :EXTERNAL)
  (list "Insufficient funding" :FUNDING)
  (list "Unequivocal evidence of treatment benefit" :HIGHBENEFIT)
  (list "Lack of compliance in a large number of patients" :LOWCOMPLIANCE)
  (list "Failure to include enough patients at a sufficient rate" :LOWENROLLMENT)
  (list "Personnel issues, like Principal Investigator change" :PERSONNEL)
  (list "Unequivocal evidence of treatment harm" :SAEISSUE)
  (list "Business reasons" :STRATEGIC)
  (list "Unable to determine a valid classification" :UNKNOWN)
  (list "No emerging trends and no reasonable chance of benefit" :UNLIKELYBENEFIT)))

(defpartial outcome-item [{:keys [safety_issue time_frame description measure]}]
  [:li
   [:h4 (str "Measure: " measure)]
   [:p (str "Safety Issue: " safety_issue " Time frame: " time_frame)]
   [:p (str "Description: " description)]])

(defpartial outcomes-list [items]
  [:ul (map outcome-item items)])

(defpartial classify-trial [trial]
     [:h2 (str "Please select the appropriate category for trial "
               (:id trial))]
     (form-to [:post "/trials/classify"]
              (drop-down "stopped_class" trial-class-map)
              (hidden-field "id" (:id trial))
              (hidden-field "why_stopped" (:why_stopped trial))
              (submit-button "Classify Trial"))
     [:p [:span.label "Overall Status: "] (:overall_status trial)]
     [:p [:span.label "Why Stopped: " ] (:why_stopped trial)]
     [:p [:span.label "Detailed Description: "]
      (:textblock (:detailed_description trial))]
     [:p [:span.label "Lead Sponsor: "]
      (:agency (:lead_sponsor (:sponsors trial))) " ("
      (:agency_class (:lead_sponsor (:sponsors trial))) ")"]
     (if (> 0 (count (:primary_outcome trial)))
       [:h5 "Primary Outcomes: "]
       (outcomes-list (:primary_coutcome trial)))
     (if (> 0 (count (:secondary_outcome trial)))
       [:h5 "Secondary Outcomes: "]
       (outcomes-list (:secondary_outcome trial))))

(defpage [:get "/trials/classify/random"] []
  (common/layout
   [:h1 "Help us classify clinical trials"]
   [:p "With smart humans categorizing clinical trials..."]
   (classify-trial (random-stopped-trial))))

(defpage [:get "/trials/classify/thanks"] []
  (common/layout
   [:h1 "Thanks for helping us classify these trials. Try another?"]
   (classify-trial (random-stopped-trial))))

(defpage [:get ["/trials/classify/:id" :id #"NCT\d+"]] {:keys [id]}
  (common/layout (classify-trial
                  (get-trial "http://api.lillycoi.com/v1" id))))