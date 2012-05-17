(ns ogrim.parsers.ssb
  (:use [net.cgrand.enlive-html])
  (:require [ogrim.common.downloader :as d]
            [ogrim.common.db :as db]))

(def ^:dynamic *db*
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname  "//localhost:5432/norge-digitalt"
   :user "postgres"
   :password ""})

(defn str->map
  "Reads a html as string and returns a map"
  [string]
  (first (-> string java.io.StringReader. html-resource)))

(defn all-links
  "Returns all links from the html-map by a given selector"
  [html-map selector]
  (distinct (map #(:href (:attrs %)) (select html-map selector))))

(defn parse-names [html-map]
  (filter #(:bgcolor (:attrs %))
          (:content (first (:content (second (:content html-map)))))))

(defn parse-node [node]
  (let [[name number] (:content node)]
    [(.toLowerCase (first (:content name))) (first (:content number))]))

(defn node->url [node] (str "http://www.ssb.no/navn/alf/" node))

(defn scrape []
  (->> (drop 1 (all-links (str->map (d/download
                                     "http://www.ssb.no/navn/alf/main.html"
                                     "ISO-8859-1"))
                          [:ul > :a]))
       (map node->url)
       (map #(d/download % "ISO-8859-1"))
       (map #(parse-names (str->map %)))
       (mapcat #(map parse-node %))))

(defn insertion [names]
  (let [i (ref 0)]
    (doseq [[name antall] names]
      (db/insert-row *db* "ssb.etternavn"
                     [(dosync (alter i inc)) name (Integer/parseInt antall)]))))




(def jenter "http://www.ssb.no/navn/jenter2001-2010.html")
(def gutter "http://www.ssb.no/navn/gutter2001-2010.html")
(def j (str->map (d/download jenter
                      "ISO-8859-1")))
(def g (str->map (d/download gutter
                      "ISO-8859-1")))
(def jn (remove nil? (map #(first (:content (first (:content %)))) (:content (last (:content (nth (:content (second (:content j))) 3)))))))
(def gn (remove nil? (map #(first (:content (first (:content %)))) (:content (last (:content (nth (:content (second (:content g))) 3)))))))
(def j1 (map #(.toLowerCase %) jn))
(def g1 (map #(.toLowerCase %) gn))
