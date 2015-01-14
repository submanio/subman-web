(ns subman-web.helpers-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test :refer-macros [deftest testing done is]]
            [test-sugar.core :refer [is=]]
            [cljs.core.async :refer [<!]]
            [subman-web.helpers :as helpers]))

(deftest test-is-filled?
  (testing "not when nil"
    (is= false (helpers/is-filled? nil)))
  (testing "not when blank"
    (is= false (helpers/is-filled? "")))
  (testing "or yes"
    (is= true (helpers/is-filled? "test"))))

(deftest test-add-0-if-need
  (testing "if length = 1"
    (is= "03" (helpers/add-0-if-need "3")))
  (testing "if length = 1 and number passed"
    (is= "03" (helpers/add-0-if-need 3)))
  (testing "not if other lenght"
    (is= "12" (helpers/add-0-if-need "12"))))

(deftest test-format-season-episode
  (testing "if only season"
    (is= "S02" (helpers/format-season-episode 2 nil)))
  (testing "if only episode"
    (is= "E12" (helpers/format-season-episode nil 12)))
  (testing "if season and episode"
    (is= "S12E02" (helpers/format-season-episode 12 2)))
  (testing "if nothing"
    (is= "" (helpers/format-season-episode nil nil))))

(deftest ^:async test-atom-to-chan
  (let [atm (atom 0)
        chn (helpers/atom-to-chan atm)]
    (go (is (= (<! chn) 0))
        (reset! atm 10)
        (is (= (<! chn) 10))
        (swap! atm inc)
        (is (= (<! chn) 11))
        (done))))

(deftest ^:async test-subscribe-to-state
  (let [state (atom {})
        ch (helpers/subscribe-to-state state :options :lang)]
    (go (testing "send value when added"
          (swap! state assoc :options {:lang "english"})
          (is (= (<! ch) "english")))
        (testing "send value when changed"
          (swap! state assoc-in [:options :lang] "spanish")
          (is (= (<! ch) "spanish")))
        (testing "return blank string when nil"
          (swap! state assoc-in [:options :lang] nil)
          (is (= (<! ch) "")))
        (done))))
