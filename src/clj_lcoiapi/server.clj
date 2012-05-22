(ns clj-lcoiapi.server
  (:require [noir.server :as server]
            [ring.middleware.gzip :as gzip]))

(server/load-views "src/clj_lcoiapi/views/")
(server/add-middleware gzip/wrap-gzip)

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'clj-lcoiapi})))

