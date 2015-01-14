(ns subman-web.helpers
  (:require [clojure.tools.logging :as log]
            [subman-web.const :as const]))

(defn make-safe
  "Make fnc call safe"
  [fnc fallback]
  (fn [& args]
    (try (apply fnc args)
         (catch Exception e (do (log/debug e (str "When called " fnc " with " args))
                                fallback)))))

(defmacro defsafe
  "Define safe function"
  [name & body]
  (if (string? (first body))
    `(defsafe ~name ~@(rest body))
    `(def ~name (make-safe (fn ~@body)
                           nil))))

(defn remove-first-0
  "Remove first 0 from string"
  [query]
  (clojure.string/replace query #"^(0+)" ""))

(defn make-static
  "Make paths static"
  [& paths]
  (map #(str const/static-path %) paths))

(defn as-static
  "Call as static"
  [callable & paths]
  (apply callable (apply make-static paths)))

(defn -with-atom
  [atm-vals fnc]
  (let [pairs (partition 2 atm-vals)
        origs (doall (map #(deref (first %)) pairs))]
    (doseq [[atm val] pairs]
      (reset! atm val))
    (let [result (fnc)]
      (doseq [[[atm _] orig] (map vector pairs origs)]
        (reset! atm orig))
      result)))

(defmacro with-atom
  "With redefined atom value"
  [atm-vals & body]
  `(-with-atom ~atm-vals
               (fn [] ~@body)))

(defn chunked-pmap
  "Like pmap but runs on chunks of data"
  [fnc chunk-size coll]
  (->> (partition-all chunk-size coll)
       (pmap (comp doall (partial map fnc)))
       (apply concat)))
