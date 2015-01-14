(ns subman-web.handlers
  (:require [compojure.handler :as handler]
            [hiccup.middleware :refer [wrap-base-url]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-body]]
            [subman-web.routes :as routes]
            [subman-web.models :refer [connect!]]))

(def app (-> (handler/site routes/main-routes)
             (wrap-transit-response {:encoding :json})
             (wrap-transit-body {:encoding :json})
             wrap-base-url
             wrap-reload))

(defn init
  "Init ring handler"
  []
  (connect!))
