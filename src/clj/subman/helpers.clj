(ns subman.helpers
  (:import (java.io StringReader))
  (:require [clojure.stacktrace :refer [print-cause-trace]]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [subman.const :as const]))

(defn remove-first-0
  "Remove first 0 from string"
  [query]
  (clojure.string/replace query #"^(0+)" ""))

(defn get-from-line
  "Get parsed html from line"
  [line]
  (html/html-resource (StringReader. line)))

(defn fetch
  "Fetch url content"
  [url]
  (-> (client/get url {:socket-timeout const/conection-timeout
                       :conn-timeout   const/conection-timeout})
      :body
      get-from-line))

(defn nil-to-blank
  "Replace nil with blank string"
  [item]
  (if (nil? item)
    ""
    item))

(defn make-safe
  "Make fnc call safe"
  [fnc fallback]
  (fn [& args]
    (try (apply fnc args)
         (catch Exception e (do ;(log/warn e (str "When called " fnc " with " args))
                                fallback)))))

(defmacro defsafe
  "Define safe function"
  [name & body]
  (if (string? (first body))
    `(defsafe ~name ~@(rest body))
    `(def ~name (make-safe (fn ~@body)
                           nil))))

(defn get-season-episode
  "Add season and episode filters"
  [text]
  (if-let [[_ season episode] (re-find #"[sS](\d+)[eE](\d+)" text)]
    [(remove-first-0 season)
     (remove-first-0 episode)]
    ["" ""]))

(defn get-from-file
  "Get parsed html from file"
  [path]
  (html/html-resource (StringReader.
                        (slurp path))))

(defn make-static
  "Make paths static"
  [& paths]
  (map #(str const/static-path %) paths))

(defn as-static
  "Call as static"
  [callable & paths]
  (apply callable (apply make-static paths)))
