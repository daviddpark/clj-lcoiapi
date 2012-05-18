(ns clj-lcoiapi.views.api
  (:require [clj-lcoiapi.views.common :as common]
            [noir.response :as response])
  (:use [noir.core :only [defpage]]
        [clj-lcoiapi.core :only [classify-trials parse-study-design-all-trials]]
        [hiccup.core :only [html]]
        ))

(defpage "/test" []
  (response/json {:test "success"}))

(defpage [:get "/trials/stopped"] []
  (response/json (classify-trials)))

(defpage [:get "/trials/studydesign"] []
  (response/json (parse-study-design-all-trials)))


