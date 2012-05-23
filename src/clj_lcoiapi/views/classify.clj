(ns clj-lcoiapi.views.classify
  (:require [clj-lcoiapi.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage defpartial]]
        [clj-lcoiapi.core :only [get-trial random-stopped-trial]]
        [hiccup.core :only [html]]
        [hiccup.form]))

(def trial-class-map (list (list " -- Please select a category --" "")
  (list "AEISSUE: Unexpected unacceptable side effects" :AEISSUE)
  (list "COMPLETED: More accurately described as completed" :COMPLETED)
  (list "DESCRIPTION: The reason for stopping may be in the description" :DESCRIPTION)
  (list "DESIGNCHANGE: The trial was stopped due to changes in the study design" :DESIGNCHANGE)
  (list "EXTERNAL: The trial was stopped due to external factors" :EXTERNAL)
  (list "FUNDING: Insufficient funding" :FUNDING)
  (list "HIGHBENEFIT: Unequivocal evidence of treatment benefit" :HIGHBENEFIT)
  (list "LOWCOMPLIANCE: Lack of compliance in a large number of patients" :LOWCOMPLIANCE)
  (list "LOWENROLLMENT: Failure to include enough patients at a sufficient rate" :LOWENROLLMENT)
  (list "PERSONNEL: Personnel issues, like Principal Investigator change" :PERSONNEL)
  (list "SAEISSUE: Unequivocal evidence of treatment harm" :SAEISSUE)
  (list "STRATEGIC: Business reasons" :STRATEGIC)
  (list "UNKNOWN: Unable to determine a valid classification" :UNKNOWN)
  (list "UNLIKELYBENEFIT: No emerging trends and no reasonable chance of benefit" :UNLIKELYBENEFIT)))

(defpartial outcome-item [{:keys [safety_issue time_frame description measure]}]
  [:li
   (if (not (nil? measure))
     [:p [:span.label "Measure: "] measure])
   (if (not (nil? safety_issue))
     [:p [:span.label "Safety Issue: "]  safety_issue])
   (if (not (nil? time_frame))
     [:p [:span.label "Time frame: "] time_frame])
   (if (not (nil? description))
     [:p [:span.label "Description: "] description])])

(defpartial primary-outcomes-list [items]
  [:h4 "Primary Outcomes: "]
  [:ul (map outcome-item items)])

(defpartial secondary-outcomes-list [items]
  [:h4 "Secondary Outcomes: "]
  [:ul (map outcome-item items)])

(defpartial classify-trial [trial]
  [:h2 (str "Please select the appropriate category for trial "
            (:id trial))]
  [:p [:span.label "Projected Classification: "
       (:best-category (:why_stopped_classification trial))]]
  (form-to [:post "/trials/classify"]
           (if (nil? (:why_stopped trial))
             (list [:p [:span.label "Since the reason why this trial was stopped was not specified explicitly, if you are able to determine a reason why it was stopped from either the detailed description or brief summary fields, please copy that text here: " ]]
                   (text-area {:rows 3, :cols 100} "why_stopped"))
             (hidden-field "why_stopped" (:why_stopped trial)))
           (hidden-field "id" (:id trial))
           (drop-down "stopped_class" trial-class-map)
           (submit-button "Classify Trial")
           )
  [:p [:span.label "Overall Status: "] (:overall_status trial)]
  [:p [:span.label "Why Stopped: " ]
   (if (nil? (:why_stopped trial))
     "[NO VALUE]"
     (:why_stopped trial))]
   
  (if (not (nil? (:brief_summary trial)))
    [:p [:span.label "Brief Summary: "]
     (:textblock (:brief_summary trial))])
  (if (not (nil? (:detailed_description trial)))
    [:p [:span.label "Detailed Description: "]
     (:textblock (:detailed_description trial))])
  [:p [:span.label "Lead Sponsor: "]
   (:agency (:lead_sponsor (:sponsors trial))) " ("
   (:agency_class (:lead_sponsor (:sponsors trial))) ")"]
  (if (> (count (:primary_outcome trial)) 0)
    (primary-outcomes-list (:primary_outcome trial)))
  (if (> (count (:secondary_outcome trial)) 0)
    (secondary-outcomes-list (:secondary_outcome trial))))

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