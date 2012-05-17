(ns clj-lcoiapi.views.welcome
  (:require [clj-lcoiapi.views.common :as common]
            [noir.response :as response]
            [noir.content.getting-started])
  (:use [noir.core :only [defpage]]
        [clj-lcoiapi.core :only [classify-trials-and-parse-study-design]]
        [hiccup.core :only [html]]))

(defpage "/welcome" []
         (common/layout
          [:p "Welcome to clj-lcoiapi"]))

(defpage "/test" []
  (response/json {:test "wtf"}))

(defpage [:get "/trials/stopped"] []
  (response/json (classify-trials-and-parse-study-design)))
