(ns clj-lcoiapi.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page :only [html5 include-css include-js]]))

(defpartial layout [& content]
  (html5
   [:head
    [:title "clj-lcoiapi"]
    (include-css "/css/boilerplate.css")
    (include-css "/css/lcoiapi.css")
    (include-css "/css/dot-luv/jquery-ui-1.8.20.custom.css")
    (include-js "/js/lcoiapi.js")
    (include-js
     "http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js")
    (include-js "/js/jquery-ui-1.8.20.custom.min.js")
    (include-js "https://www.google.com/jsapi")]
   [:body
    [:div#wrapper
     content]]))
