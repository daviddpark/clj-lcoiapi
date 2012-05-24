(ns clj-lcoiapi.views.classify
  (:require [clj-lcoiapi.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage defpartial]]
        [clj-lcoiapi.core :only [get-trial random-stopped-trial]]
        [hiccup.core :only [html]]
        [hiccup.form]))

(defpartial outcome-item [{:keys [safety_issue time_frame description measure]}]
  [:li
   (if (not (nil? measure))
     [:p [:div.label "Measure: "] measure])
   (if (not (nil? description))
     [:p [:div.label "Description: "] description])
   [:p
   (if (not (nil? safety_issue))
     [:span.label "Safety Issue: "  safety_issue])
   (if (not (nil? time_frame))
     [:span.label " Time frame: " time_frame])]
   ])
   
(defpartial primary-outcomes-list [items]
  [:h4 "Primary Outcomes: "]
  [:ul.outcomes (map outcome-item items)])

(defpartial secondary-outcomes-list [items]
  [:h4 "Secondary Outcomes: "]
  [:ul.outcomes (map outcome-item items)])

(defpartial trial-common [trial]
  [:h3 (:id trial) " - " (:brief_title trial)]
  [:p [:span.label "Overall Status: "] (:overall_status trial)]
  [:p [:span.label "Why Stopped: " ]
   (if (nil? (:why_stopped trial))
     "[NO VALUE]"
     [:span#why_stopped (:why_stopped trial)]
     )])

(defpartial probabilities [trial]
  [:h1 [:a#probabilities "What the machine thinks of this trial's stopped reason"]]
  [:div
   [:p [:div.label "Why Stopped: " ]
    [:span#why_stopped (:why_stopped trial)]]
   [:p [:div.label "Best Category: "]
    (:best-category (:why_stopped_classification trial))]
   [:div#probChartDiv]
   [:h3 "Probabilities for each category:"]
   (let [probs (:probabilities (:why_stopped_classification trial))
         sortedprobs (into (sorted-map-by (fn [k1 k2] (<= (get probs k2) (get probs k1)))) probs)]
     (for [key (keys sortedprobs)]
       [:p [:div.label key ": "]
        [:span.probability (get probs key)]]))
   
   [:h4 "What does this tell us?"]
   [:p "The concept of 'Best Category' is essentially the classification that has "
    "the highest probabilty of being correct. If the probabilities are relatively "
    "close to each other mathematically, rather than a single best category, we "
    "could determine multiple derived reasons for stopping."]
   ]
  [:script {:type "text/javascript"}
   "google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});\n"
   "google.setOnLoadCallback(drawChart);\n"
   "function drawChart() {\n"
   "  var data = google.visualization.arrayToDataTable([\n"
   "    ['Category', 'Probability (%)']"
   (let [probs (:probabilities (:why_stopped_classification trial))]
     (apply str (for [cat (keys probs)]
                  (str ",['" cat "', " (get probs cat) "]"))))
   "    ]);\n"
   "  var options = { title: 'Category Probabilities'};\n"
   "  var chart = new google.visualization.PieChart(document.getElementById('probChartDiv'));\n"
   "  chart.draw(data, options);\n"
   "}"
   ]
  )

(defpartial classify-trial [trial]
  [:div#accordion
   [:h1 [:a#howtohelp "How you can help"]]
   [:div
    [:p "Below are some fields with text that might be used to "
     "identify a category. If you use your mouse to select the "
     "specific words that gave you the insight as to how you "
     "would classify the reason why the study was terminated, "
     "we will show you a popup and you can choose which category "
     "you would associate with the words you selected. "
     "You can select as many phrases and classify them individually "
     "as needed."]
    [:p "We will take your classifications and use them to create "
     "a training set that can be applied to the 'Why Stopped' field, "
     "so that we can apply probabilistic analysis to determine the "
     "best possible category for studies that have not been "
     "explicitly categorized."]
    ]
   [:h1 [:a#trialDetailLink (str "Classify trial " (:id trial))]]
   [:div
    [:div {:id "annotationz"}
     [:div {:id "annotationClass"} [:h5 "CATEGORY"]]
     [:div {:id "annotationText"} [:h5 "SELECTED TEXT"]]
     [:div {:id "annotationUrlClass"} [:h5 "CATEGORY"]]
     [:div {:id "annotationUrlText"} [:h5 "RELEVANT URL"]]
     (form-to {:id "classificationForm"}
              [:post "/trials/classify"]
              (if (nil? (:why_stopped trial))
                [:p [:div.label
                     "Since the reason why this trial was stopped was not specified "
                     "explicitly, if you are able to determine a reason why it was "
                     "stopped from either the detailed description or brief summary "
                     "fields, please select text from those fields."]]
                )
              (hidden-field "id" (:id trial))
              (hidden-field {:id "annotationJson"} "annotationJson" "[]")
              (hidden-field {:id "annotationUrlJson"} "annotationUrlJson" "[]")
              (submit-button {:id "submitAnnotation"} "Submit")
              (reset-button {:id "cancelSubmission"} "Cancel")
              )]
    
    [:div#trialDetails
     (trial-common trial)
     (if (not (nil? (:brief_summary trial)))
       [:p.hiliteAnnotation [:span.label "Brief Summary: "]
        (:textblock (:brief_summary trial))])
     (if (not (nil? (:detailed_description trial)))
       [:p.hiliteAnnotation [:span.label "Detailed Description: "]
        (:textblock (:detailed_description trial))])]
    ]
   [:h1 [:a#additional "Additional Trial Detail"]]
   [:div
    (trial-common trial)
    [:p "First Received on " (:value (:firstreceived_date trial))
     " Last Updated on " (:value (:lastchanged_date trial))]
    [:p [:span.label "Lead Sponsor: "]
     (:agency (:lead_sponsor (:sponsors trial))) " ("
     (:agency_class (:lead_sponsor (:sponsors trial))) ")"]
    (for [collaborator (:collaborator (:sponsors trial))]
      [:p [:span.label "Collaborator: "]
       (:agency collaborator) " ("
       (:agency_class collaborator) ")"])
    (if (not (nil? (:responsible_party trial)))
      [:p [:span.label "Responsible Party: "]
     (let [responsible-party (:responsible_party trial)]
       (str (:investigator_full_name responsible-party) " "
            (:investigator_affiliation responsible-party)))])
    [:div#condition
     [:h4 "Conditions"]
     [:ul#condition
     (for [condition (:mesh_term (:condition_browse trial))]
       [:li condition])]]
    [:div#intervention
     [:h4 "Interventions"]
     [:ul#intervention
     (for [intervention (:mesh_term (:intervention_browse trial))]
       [:li intervention])]]
    [:div#phase
     [:h4 "Phase"]
     (:phase trial)]
    [:p [:span.label "Study Type: "] (:study_type trial)]
    [:h4 "Study Design"]
    [:ul (for [key (keys (:study_design_facets trial))]
           [:li [:span.label key ": "] (get (:study_design_facets trial) key)])]
    [:p [:span.label "Official Title: " ](:official_title trial)]
    (if (> (count (:primary_outcome trial)) 0)
      (primary-outcomes-list (:primary_outcome trial)))
    (if (> (count (:secondary_outcome trial)) 0)
      (secondary-outcomes-list (:secondary_outcome trial)))
    [:p [:span.label "Enrollment: "] (:value (:enrollment trial))]
    [:p [:span.label "Study Start Date: "] (:value (:start_date trial))]
    [:p [:span.label "Study Completion Date: "] (:value (:completion_date trial))]
    [:p [:span.label "Primary Completion Date: "] (:value (:primary_completion_date trial))]
    ]

   (if (not (nil? (:why_stopped trial)))
     (probabilities trial))
   ]
  [:script {:type "text/javascript"}
   "$(document).ready(function(){classifyLoaded()});"
   ]
  [:div#classify_dialog])

(defpage [:get "/trials/classify/random"] []
  (common/layout
   [:h1 "Help us classify clinical trials"]
   [:p "We are applying machine learning techniques to try to "
    "categorize the reasons why a given study has been "
    "terminated, suspended or withdrawn. With your help, "
    "we are building up a training set by identifying the "
    "keywords that can be used to infer a given classification."]
   (classify-trial (random-stopped-trial))))

(defpage [:get "/trials/classify/thanks"] []
  (common/layout
   [:h1 "Thanks for helping us classify these trials. Try another?"]
   (classify-trial (random-stopped-trial))))

(defpage [:get ["/trials/classify/:id" :id #"NCT\d+"]] {:keys [id]}
  (common/layout (classify-trial
                  (get-trial "http://api.lillycoi.com/v1" id))))

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

