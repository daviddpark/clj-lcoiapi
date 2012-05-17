(ns clj-lcoiapi.test.core
  (:use [clj-lcoiapi.core])
  (:use [clojure.test]))

(deftest test-construct-terminated-url
  (is (= (str "http://someplace.com/v1/trials/search?fields=foo,bar,baz&query=show_xprt:Y,"
              "xprt:%28+%28Terminated%29+OR%28Suspended%29+OR+%28Withdrawn%29+%29+%5BOVERALL-STATUS%5D,count:999999")
         (construct-terminated-url "http://someplace.com/v1" ["foo","bar","baz"]))))

(deftest test-extract-study-design-facets
  (is (= (extract-study-design-facets (str "Allocation:  Randomized, "
               "Endpoint Classification:  Safety/Efficacy Study, "
               "Intervention Model:  Parallel Assignment, "
               "Masking:  Double Blind (Subject, Caregiver, Investigator), "
               "Primary Purpose:  Prevention"))
         {"Allocation" "Randomized",
          "Endpoint Classification" "Safety/Efficacy Study",
          "Intervention Model" "Parallel Assignment",
          "Masking" "Double Blind (Subject, Caregiver, Investigator)",
          "Primary Purpose" "Prevention"})))
