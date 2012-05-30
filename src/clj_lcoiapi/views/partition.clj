(ns clj-lcoiapi.views.partition
  (:use [noir.core :only [defpage defpartial]]
        [hiccup.page :only [html5 include-css include-js]]
        [hiccup.form]
        [clj-lcoiapi.core :only
         [parse-study-design-all-trials prepare-trial-facets trial-facets]]))

(defpartial layout [& content]
  (html5
   [:head
    [:title "lcoiapi visualizations"]
    (include-css "/css/boilerplate.css")
    (include-css "/css/lcoiapi.css")
    (include-css "/css/dot-luv/jquery-ui-1.8.20.custom.css")
    (include-js "/js/lcoiapi.js")
    (include-js "/js/d3.v2.min.js")
    (include-js
     "http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js")
    (include-js "/js/jquery-ui-1.8.20.custom.min.js")
    (include-js "https://www.google.com/jsapi")]
   [:body
    [:div#wrapper
     content]]))

(defpartial facet-selector [hierarchy]
  [:div#facetSelector
   (drop-down {:id "facetSelector" :size 4} "facetSelector" [])
   (drop-down {:id "selectedFacets" :size 4} "selectedFacets" [])])

(defpage [:get ["/trials/visualize/partition"]] {:keys [hierarchy]}
  (layout
   [:h1 "Stopped Trials"]
   [:div {:id "body"}]
   [:script {:src "/js/partition.js" :type "text/javascript"}]
   ))

(defpage [:get ["/trials/visualize/treemap"]] {:keys [hierarchy]}
  (layout
   [:h1 "Stopped Trials"]
   [:div {:id "body"}]
   [:script {:src "/js/treemap.js" :type "text/javascript"}]))

(defn gtreemap-data [parent children]
  (apply str (for [child children]
               (let [childid (str parent " || " (:name child))]
                 (str "\t['" childid "',\t'" parent "', "
                      (if (not (nil? (:size child))) (:size child) 0)
                      ", "
                      (if (not (nil? (:size child))) (:size child) 0)
                      "],\n"
                      (if (not (nil? (:children child)))
                        (gtreemap-data childid (:children child))))))))

(defpage [:get ["/trials/visualize/gtreemap"]] {:keys [hierarchy]}
  (layout
   [:h1 "Stopped Trials"]
   ;;(facet-selector hierarchy)
   [:div {:id "chart_div"}]
   [:script {:type "text/javascript"}
    "google.load('visualization', '1', {packages:['treemap']});\n"
    "google.setOnLoadCallback(drawChart);\n"
    "function drawChart() {\n"
    "  var data = google.visualization.arrayToDataTable([\n"
    "\t['Facet', 'Parent', 'Number of Trials', 'Number of Trials'],\n"
    "\t['Stopped Trials', null, 0, 0],\n"
    (apply str (for [child (trial-facets
                            (map keyword (clojure.string/split hierarchy #","))
                            (prepare-trial-facets))]
                 (str "\t['" (:name child) "',\t'Stopped Trials', 0, 0],\n"
                      (gtreemap-data (:name child) (:children child)))))
    
    
    "  ]);\n"
    "var tree = new google.visualization.TreeMap(document.getElementById('chart_div'));\n"
    "tree.draw(data, {minColor: '#f00',midColor: '#ddd',maxColor: '#0d0',\n"
    "                 headerHeight: 15,fontColor: 'black',showScale: true});\n"
    "}\n"
    "$(document).ready(prepareFacetSelector);"
    ]
   )
  )