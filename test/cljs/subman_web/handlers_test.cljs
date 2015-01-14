(ns subman-web.handlers-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clj-di.core :refer [let-deps]])
  (:require [cemerick.cljs.test :refer-macros [deftest done testing is]]
            [cljs.core.async :refer [<! timeout]]
            [clj-di.core :refer [register!]]
            [subman-web.helpers :refer [DummyHistory]]
            [subman-web.const :as const]
            [subman-web.models :as m]
            [subman-web.handlers :as h]))

(deftest ^:async test-handle-search-query!
  (let [state (atom {:search-query "initial"
                     :stable-search-query ""})]
    (h/handle-search-query! state)
    (go (testing "set initial value"
          (= (:stable-search-query @state) "initial"))
        (testing "not change stable search query eager"
          (swap! state assoc
                 :search-query "new search query")
          (<! (timeout (/ const/input-timeout 2)))
          (= (:stable-search-query @state) "initial"))
        (testing "change stable search query after timeout"
          (<! (timeout const/input-timeout))
          (= (:stable-search-query @state) "new search query"))
        (done))))

(deftest ^:async test-handle-stable-search-query!
  (let [search-url (atom "")
        state (atom {:stable-search-query ""})
        options (atom {:source "all"
                       :language "english"})]
    (register! :history (DummyHistory. ""))
    (h/handle-stable-search-query! state options)
    (go (register! :http-get (fn [url]
                               (reset! search-url url)
                               (go {:body [{:test :test}]})))
        (let-deps [history :history]
          (testing "do nothing without search query change"
            (swap! state assoc
                   :offset 10)
            (is (= @search-url ""))
            (is (= (.-token history ""))))
          (swap! state assoc
                 :stable-search-query "test-query")
          (<! (timeout 1000))
          (testing "call server when query changed"
            (is (re-find #"test-query" @search-url)))
          (testing "update search result"
            (is (= (:results @state) [{:test :test}])))
          (testing "reset offset"
            (is (= (:offset @state) 0)))
          (testing "change url with query"
            (is (= (.-token history "/search/test-query"))))
          (testing "change url with blank query"
            (swap! state assoc
                   :stable-search-query "")
            (<! (timeout 1000))
            (is (= (.-token history "/"))))
          (done)))))

(deftest ^:async test-handle-total-count!
  (let [state (atom {})]
    (go (register! :http-get (fn [_]
                               (go {:body {:total-count 9999}})))
        (h/handle-total-count! state)
        (<! (timeout 1000))
        (is (= (:total-count @state) 9999))
        (done))))

(deftest ^:async test-handle-single-option!
  (let [state (atom {:options {:option {:value ""}}})
        options (atom {:option "value"})]
    (go (h/handle-single-option! state options :option)
        (<! (timeout 1000))
        (testing "set first non blank value"
          (is (= (:option @options)
                 (get-in @state [:options :option :value])
                 "value")))
        (testing "update state when options changed"
          (swap! options assoc
                 :option "option-value")
          (<! (timeout 1000))
          (is (= (:option @options)
                 (get-in @state [:options :option :value])
                 "option-value")))
        (testing "update options when state changed"
          (swap! state assoc-in
                 [:options :option :value] "state-value")
          (<! (timeout 1000))
          (is (= (:option @options)
                 (get-in @state [:options :option :value])
                 "state-value")))
        (done))))

(deftest ^:async test-handle-options!
  (go (register! :http-get (fn [_]
                             (go {:body [{:term "english"}
                                         {:term "china"}]})))
      (register! :sources {const/type-addicted "Addicted"
                           const/type-opensubtitles "opensubtitles"
                           const/type-all "All"})
      (let [state (atom {})
            options (atom {:language const/default-language
                           :source (get const/type-names
                                        const/default-type)})]
        (h/handle-options! state options)
        (<! (timeout 1000))
        (testing "set default values"
          (is (= (:source @options)
                 (get-in @state [:options :source :value])
                 (get const/type-names
                      const/default-type)))
          (is (= (:language @options)
                 (get-in @state [:options :language :value])
                 const/default-language)))
        (testing "fill available values"
          (is (= (apply hash-set
                        (get-in @state [:options :language :options]))
                 #{"english" "china"}))
          (is (= (apply hash-set
                        (get-in @state [:options :source :options]))
                 #{"addicted" "opensubtitles" "all"}))))
      (done)))
