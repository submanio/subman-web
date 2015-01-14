(ns subman-web.models-test
  (:require [clojure.test :refer [deftest testing]]
            [clojurewerkz.elastisch.rest.document :as esd]
            [test-sugar.core :refer [is= is-do]]
            [subman-web.helpers :refer [with-atom]]
            [subman-web.const :as const]
            [subman-web.models :as models]
            [monger.collection :as mc]
            [monger.core :as mg]))

(deftest test-get-total-count
  (with-redefs [esd/search (constantly {:hits {:total 10}})]
    (is= (models/get-total-count) 10)))

(deftest test-get-season-episode-parts
  (testing "with SnEn notation"
    (is= (#'models/get-season-episode-parts "test s01e10")
         ["s01e10" "01" "10"]))
  (testing "with nxn notation"
    (is= (#'models/get-season-episode-parts "test 01x10")
         ["01x10" "01" "10"]))
  (testing "or nil"
    (is-do nil? (#'models/get-season-episode-parts "test"))))

(deftest test-get-season-episode
  (testing "with season and episode"
    (is= (#'models/get-season-episode "test s01e01")
         [{:term {:season "1"}} {:term {:episode "1"}}]))
  (testing "without"
    (is= (#'models/get-season-episode "test") [])))

(deftest test-build-query
  (testing "should build query"
    (is= (#'models/build-query "Dads.2013.S01E18.HDTV.x264-EXCELLENCE[rartv]"
           "en" const/type-all const/result-size)
         [:query {:bool {:must
                         [{:fuzzy_like_this
                           {:boost 5
                            :fields [:show :name]
                            :like_text "Dads 2013 S01E18 HDTV x264-EXCELLENCE[rartv]"}}
                          {:term {:season "1"}}
                          {:term {:episode "18"}}]
                         :should {:fuzzy_like_this
                                  {:boost 2
                                   :fields [:version]
                                   :like_text "Dads 2013 S01E18 HDTV x264-EXCELLENCE[rartv]"}}}}
          :filter {:term {:lang "en"}} :size 100]))
  (testing "should build query with filter by source"
    (is= (#'models/build-query "query" "ru" const/type-addicted const/result-size)
         [:query {:bool {:must
                         [{:fuzzy_like_this
                           {:boost 5
                            :fields [:show :name]
                            :like_text "query"}}
                          {:term {:source 0}}]
                         :should {:fuzzy_like_this
                                  {:boost 2
                                   :fields [:version]
                                   :like_text "query"}}}}
          :filter {:term {:lang "ru"}} :size 100])))

(deftest test-search
  (with-redefs [esd/search (fn [_ _ _ & {:keys [from size] :as _}]
                             (when (and (= from 10) (= size const/result-size))
                               {:hits {:hits [{:_source "test"}]}}))]
    (is= ["test"] (models/search :query "test"
                                 :offset 10
                                 :lang "en"
                                 :limit const/result-size))))

(deftest test-list-languages
  (with-redefs [esd/search (fn [& _] {:facets {:tag {:terms [{:term "english"
                                                              :count 100}
                                                             {:term "russian"
                                                              :count 50}]}}})]
    (is= (models/list-languages) [{:term "english"
                                   :count 100}
                                  {:term "russian"
                                   :count 50}])))

(deftest test-get-show-season-episode-set
  (with-redefs [esd/search (fn [& _] {:hits {:hits [{:fields {:show ["american dad"]
                                                              :season ["10"]
                                                              :episode ["23"]}}]}})]
    (is= (models/get-show-season-episode-set 0)
         #{'("american dad" "10" "23")})))

(deftest test-get-unique-show-season-episode
  (with-redefs [esd/search (fn [& _] {:hits {:hits [{:fields {:show ["american dad"]
                                                              :season ["10"]
                                                              :episode ["23"]}}]}})
                models/get-total-count (fn [] 10)]
    (is= (models/get-unique-show-season-episode)
         #{'("american dad" "10" "23")})))

(deftest test-update-unique-show-season-episode!
  (with-atom [models/unique-show-season-episode nil]
    (with-redefs [models/get-unique-show-season-episode (fn [] [:value])]
      (models/update-unique-show-season-episode!)
      (is= @models/unique-show-season-episode
           [[:value]]))))
