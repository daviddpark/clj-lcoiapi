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
  [:h3.hiliteAnnotation (:id trial) " - " (:brief_title trial)]
  [:p [:span.label "Overall Status: "] [:span#overall_status (:overall_status trial)]]
  [:p#why_stopped [:span.label "Why Stopped: " ]
   (if (nil? (:why_stopped trial))
     "[NO VALUE]"
     [:span#why_stopped (:why_stopped trial)]
     )])

(defpartial trial-annotations [trial]
  (for [curation (:curations trial)]
    (let [annotations (:annotations curation)]
      (if (not (empty? annotations))
        [:table
         [:tr [:td {:colspan 2}
               [:h4 "ANNOTATED CLASSIFICATIONS"]]]
         [:tr
          [:th "Classification"] [:th "Selected Text"]]
         (for [annotation annotations]
           [:tr
            [:td (first annotation)]
            [:td (first (rest annotation))]])])
      ))
  (for [curation (:curations trial)]
    (let [annotation-urls (:annotationUrls curation)]
      (if (not (empty? annotation-urls))
        [:table
         [:tr [:td {:colspan 2}
               [:h4 "EXTERNAL URL CLASSIFICATIONS"]]]
         [:tr
          [:th "Classification"] [:th "URL"]]
         (for [annotation annotation-urls]
           [:tr
            [:td (first annotation)]
            [:td [:a {:target "_blank"
                      :href (first (rest annotation))}
                  (first (rest annotation))]]])]))))

(defpartial probabilities [trial]
  [:h1 [:a#probabilities "Classification of Why Stopped"]]
  [:div
   (trial-annotations trial)
   
   [:h3 "Statistically, the classification is probably '"
    (:best-category (:why_stopped_classification trial)) "'"]
   [:p [:span.label "Why Stopped: " ]
    [:span#why_stopped (:why_stopped trial)]]
   
   [:h4 "Probability distribution"]
   [:div#probChartDiv]
      
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
   (let [probs (:probabilities (:why_stopped_classification trial))
         sortedprobs (into (sorted-map-by (fn [k1 k2] (<= (get probs k2) (get probs k1)))) probs)]
     (apply str (for [cat (keys sortedprobs)]
                  (str ",['" cat "', " (get probs cat) "]"))))
   "    ]);\n"
   "  var options = { title: 'Category Probabilities', width: 800, backgroundColor: 'black',"
   "                  legend: {textStyle: {color: '#D9D9D9'}}};\n"
   "  var chart = new google.visualization.PieChart(document.getElementById('probChartDiv'));\n"
   "  chart.draw(data, options);\n"
   "}"
   ]
  )

(defpartial classify-trial [trial]
  [:div#accordion
   [:h1 [:a#theproblem "USE CASE: Why studies end prematurely"]]
   [:ul
    [:li "There are many clinical trials that were stopped early."]
    [:li "What insight can we gain by understanding the reasons why?"]
    [:li "Sometimes, the 'Why Stopped' field is filled out"]
    [:li "As a free-text field, there are typos and the 'WHY' may not be obvious"]
    [:li "If we can classify a subset of this text into distinct categories,"
     " we can treat this subset as a training set and use NLP techniques "
     " to classify the rest of the data."]
    [:li "Explicitly annotated trials can also prioritize the human selected "
     "classification over the machine learned classification."]]


   [:h1 [:a#howtohelp "How to Help"]]
   [:div
        
    [:ul
     [:li "We will show you a randomly selected stopped trial."]
     [:li "If the sponsor has entered information into the 'Why Stopped' field:"
      [:ul
       [:li "Select the words in the field that help you choose a classification."]
       [:li "A popup will allow you to associate a classification with the selected words."]
       [:li "If the trial might be classified with multiple categories, select ONLY the words that apply to the classification, and repeat as relevant."]]
      ]
     [:li "If the trial has no 'Why Stopped' field, examine the trial details."
      [:ul
       [:li "Perhaps a Google search of some of the text in the 'Detailed Description' or 'Brief Summary' fields might prove insightful."]
       [:li "Select some text, then select the 'Google Search' button"]
       [:li "If any of the search results can provide some insight into why the trial was stopped, copy the URL and store it as a classified annotation."]]]]]
   
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
     [:span#lead_sponsor (:agency (:lead_sponsor (:sponsors trial)))]
     " (" (:agency_class (:lead_sponsor (:sponsors trial))) ")"]
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

(defpage [:get "/trials/classify/context"] []
  (common/layout
   [:div#contextAccordion
    [:h3 "INTRO: Establishing Context"]
    [:div
     [:p "Current state of http://www.clinicalcollections.org provides faceted browsing over normalized fields."]
     [:p "Once the fields are normalized, you can do lots of visualization."]
     [:p "It is nearly impossible to do visualization with free-text entry fields like Inclusion/Exclusion  - unless the information parsed and normalized."]]
    [:h3 "USE CASE: Why studies end prematurely"]
    [:div
     [:h4 "Three types of 'stopped' study"]
     [:ol
      [:li "Terminated: stopped during execution"]
      [:li "Withdrawn: did not start execution"]
      [:li "Suspended: temporarily halted, optional continuation or termination"]]
     [:h4 "NLM CTGov  collects 'Why Stopped' field"]
     [:ul
      [:li "Free-text field, not normalized into specific categories."]
      [:li "Optional field, often blank"]]
     [:h4 "Can we provide more structure by classifying this free text into specific categories?"]]
    [:h4 "TASK: Categorizing free text"]
    [:div
     [:h4 "Introducing the categories:"]
     [:p "The AHA published an article "
      [:a {:href "http://circ.ahajournals.org/content/89/6/2892"} "The early termination of clinical "
       "trials: causes, consequences, and control."]
      "They establish a table of termination categories. We have complemented these categories with "
      "categories that represent a slightly broader set."
      [:ul
       [:li "AEISSUE: Unexpected unacceptable side effects"]
       [:li "COMPLETED: More accurately described as completed"]
       [:li "DESCRIPTION: The reason for stopping may be in the description"]
       [:li "DESIGNCHANGE: The trial was stopped due to changes in the study design"]
       [:li "EXTERNAL: The trial was stopped due to external factors"]
       [:li "FUNDING: Insufficient funding"]
       [:li "HIGHBENEFIT: Unequivocal evidence of treatment benefit"]
       [:li "LOWCOMPLIANCE: Lack of compliance in a large number of patients"]
       [:li "LOWENROLLMENT: Failure to include enough patients at a sufficient rate"]
       [:li "PERSONNEL: Personnel issues, like Principal Investigator change"]
       [:li "SAEISSUE: Unequivocal evidence of treatment harm"]
       [:li "STRATEGIC: Business reasons"]
       [:li "UNKNOWN: Unable to determine a valid classification"]
       [:li "UNLIKELYBENEFIT: No emerging trends and no reasonable chance of benefit"]]]
     [:p "These categories are for demonstration purposes and require detailed review and refinement."]]
    [:h4 "ML and NLP"]
    [:div
     [:p "Universities like MIT and Stanford are offering many online courses, such as ML and NLP."]
     [:ul
      [:li [:h4 "Machine Learning"]
       "A branch of artificial intelligence is a discipline focused on algorithms "
       "to allow computers to evolve behaviors based on empirical data."]
      [:li "We take a subset of the 'Why Stopped' data and construct a training set"]
      [:li [:h4 "Natural Language Processing"]
       "A narrower field of machine learning that allows a computer to extract meaningful "
       "information from natural language, or what we have so far called 'free text'."]
      [:li [:a {:href "http://opennlp.apache.org"} "Apache's OpenNLP Project"]]
      [:li [:a {:href "http://github.com/dakrone/clojure-opennlp"} "Lee Hinman's Clojure wrapper around OpenNLP"]]
      [:li "Rudimentary 'Bag of Words' implementation"]]
     ]
    [:h4 "DEMO"]
    [:div
     [:h4 "~9k 'stopped' out of ~126k clinical studies"]
     [:h5 "limited training set == limited confidence in classification"]
     [:h5 "improves over time, and training set data considered crowd curated annotation"]
     [:ol
      [:li "Training pays off! "
       [:a {:href "/trials/classify/NCT01067235"} "Recruitment continues to be a problem"]]
      [:li "Not enough data in the training set. "
       [:a {:href "/trials/classify/NCT00700609"} "Principal Investigator vs. P.I. vs. PI"]]
      [:li "Trivial Pursuit: "
       [:a {:href "/trials/classify/NCT00589524"} "When the training set has NO idea how to classify"]]
      [:li "Do we have a winner? "
       [:a {:href "/trials/classify/NCT01185548"} "The machine's top guesses may both be correct"]]
      [:li "No 'Why Stopped', but a "
       [:a {:href "/trials/classify/NCT00145431"} "Google search proves insightful."]]
      [:li "Another close race... "
       [:a {:href "/trials/classify/NCT00385398"} "More multiple reasons for termination"]]]]
    [:h4 "Summary"]
    [:div
     [:h5 "Applied machine and human workflow to annotate and curate data"]
     [:h5 "Learning loop"]
     [:ul
      [:li "Human engagement is key"]
      [:li "Machine can learn from human engagement"]]
     [:h5 "Apply similar approach of ML/NLP with human curation to annotate and classify:"]
     [:ul
      [:li "Eligibility Critera (Inclusion/Exclusion)"]
      [:li "Incorporate New Data:"
       [:ul
        [:li "Mechanism of action of interventions"]
        [:li "Biomarkers"]
        [:li "Genetic components in research"]]]
      
      ]
     [:h5 "Leverage the crowd!"]
     [:ul
      [:li "Mechanical Turk for scale"]
      [:li "Use wisdom of crowd for confidence (validation through multiple reviews)"]
      [:li "Offset poor machine interpretation with human curation"
       [:ul
        [:li "Curation workflows to build training sets"]
        [:li "Machine Learning pass for large volume of information"]
        [:li "Additional targeted curation workflows for low volume, complex discernment, "
         "disambiguation of probability distribution"]]]]]]
     
   [:script {:type "text/javascript"}
   "$(document).ready(function(){$(\"#contextAccordion\").accordion({autoHeight:false,collapsible:true})});"
   ])
  )

(defpage [:get "/trials/classify/random"] []
  (common/layout
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

