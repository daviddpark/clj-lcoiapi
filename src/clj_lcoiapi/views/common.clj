(ns clj-lcoiapi.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page-helpers :only [include-css html5]]))

(defpartial layout [& content]
            (html5
              [:head
               [:title "clj-lcoiapi"]
               (include-css "/css/lcoiapi.css")
               (include-css "/css/boilerplate.css")]
              [:body
               [:div#wrapper
                content]]))
