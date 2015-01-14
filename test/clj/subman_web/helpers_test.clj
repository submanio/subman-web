(ns subman-web.helpers-test
  (:require [clojure.test :refer [deftest testing]]
            [test-sugar.core :refer [is= is-do]]
            [subman-web.const :as const]
            [subman-web.helpers :as helpers]))

(deftest test-remove-first-0
  (is= "1" (helpers/remove-first-0 "01")))

(deftest test-make-safe
  (is= :safe ((helpers/make-safe #(throw (Exception. %)) :safe) "danger")))

(deftest test-make-static
  (is= (helpers/make-static "test" "path") [(str const/static-path "test")
                                            (str const/static-path "path")]))

(deftest test-as-static
  (is= (helpers/as-static identity "test")
       (str const/static-path "test")))

(helpers/defsafe safe-fn
  [x y]
  (throw (Exception. (str x y))))

(deftest test-defsafe
  (is-do nil? (safe-fn 1 2)))
