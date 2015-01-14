(ns subman-web.components.core
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]
            [om-tools.core :refer-macros [defcomponent]]
            [jayq.core :refer [$]]
            [subman-web.helpers :refer [is-filled?]]
            [subman-web.components.search-input :refer [search-input]]
            [subman-web.components.search-result :refer [search-result]]
            [subman-web.components.welcome :refer [welcome]]))

(defcomponent page [app _]
  (display-name [_] "Page")
  (render [_] (html [:div.page
                     (om/build search-input app)
                     (if (is-filled? (:stable-search-query app))
                       (om/build search-result app)
                       (om/build welcome app))])))

(defn init-components
  [state]
  (om/root page state
           {:target (.get ($ :#main) 0)}))
