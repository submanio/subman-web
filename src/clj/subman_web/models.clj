(ns subman-web.models
  (:require [clojure.set :refer [union]]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest :as esr]
            [environ.core :refer [env]]
            [clj-di.core :refer [register! get-dep]]
            [subman-web.helpers :as helpers]
            [subman-web.const :as const]))

(defn connect!
  "Connect to databases"
  []
  (register! :db-connection (esr/connect (env :db-host))))

(defn get-total-count
  "Update total count of subtitles"
  []
  (-> (esd/search (get-dep :db-connection)
                  (env :index-name) "subtitle")
      :hits
      :total))

(defn- get-season-episode-parts
  "Get season episode parts"
  [text]
  (or (re-find #"[sS](\d+)[eE](\d+)" text)
      (re-find #"(\d+)[xX](\d+)" text)))

(defn- get-season-episode
  "Add season and episode filters"
  [text]
  (if-let [[_ season episode] (get-season-episode-parts text)]
    [(q/term :season (helpers/remove-first-0 season))
     (q/term :episode (helpers/remove-first-0 episode))]
    []))

(defn- get-source-filter
  "Get filter by source or blank vector"
  [source]
  (if (not= source const/type-all)
    [(q/term :source source)]
    []))

(defn- build-query
  "Build search query"
  [query lang source limit]
  (-> (let [prepared (clojure.string/replace query #"\." " ")]
        {:query (q/bool :must (concat [(q/fuzzy-like-this
                                         :like_text prepared
                                         :fields [:show :name]
                                         :boost const/show-name-boost)]
                                      (get-season-episode prepared)
                                      (get-source-filter source))
                        :should (q/fuzzy-like-this
                                  :like_text prepared
                                  :fields [:version]
                                  :boost const/version-boost))
         :filter (q/term :lang lang)
         :size limit})
      vec
      flatten))

(defn search
  "Search for documents"
  [& {:keys [query offset lang source limit]}]
  (->> (apply esd/search (get-dep :db-connection)
              (env :index-name)
              "subtitle"
              :from offset
              (build-query query lang source limit))
       :hits
       :hits
       (map :_source)))

(defn list-languages
  "List languages with count"
  []
  (-> (esd/search (get-dep :db-connection)
                  (env :index-name)
                  "subtitle"
                  :query (q/match-all)
                  :facets {:tag {:terms {:field :lang
                                         :size const/languages-limit}}})
      :facets
      :tag
      :terms))

(defn get-show-season-episode-set
  [from]
  (->> (esd/search (get-dep :db-connection)
                   (env :index-name)
                   "subtitle"
                   :fields [:show :season :episode]
                   :size const/result-size
                   :from from)
       :hits
       :hits
       (map (fn [{:keys [fields]}]
              (map #(first (% fields)) [:show :season :episode])))
       (into #{})))

(defn get-unique-show-season-episode
  "Get all unique show season episode vectors"
  []
  (let [count (get-total-count)]
    (loop [from 0
           result #{}]
      (if (> from count)
        result
        (recur (+ from const/result-size)
               (union result (get-show-season-episode-set from)))))))

(def unique-show-season-episode (atom #{}))

(defn update-unique-show-season-episode!
  []
  (reset! unique-show-season-episode (partition-all const/sitemap-size
                                                    (get-unique-show-season-episode))))
