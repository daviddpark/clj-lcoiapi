(ns clj-lcoiapi.views.welcome
  (:require [clj-lcoiapi.views.common :as common]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]))

(defpage "/welcome" []
         (common/layout
          [:p "Welcome to clj-lcoiapi"]))

