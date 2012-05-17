(ns clj-egsiona.article
  (:require [ogrim.common.db :as db]
            [clj-obt.tools :as obt]
            [clojure.string :as str]
            [clj-egsiona.tagger-provider :as tag])
  (:use [clj-egsiona db-provider]
        [ogrim.common.tools]))

(def ^:dynamic *db* (get-db))

(def unvanted-tags
  ["verb" "konj" "clb" "prep" "<klokke>" "<strek>"
   "pron" "det" "ub" "inf-merke" "adv" "<parentes-beg>"
   "<parentes-slutt>" "<komma>" "sbu"])

(defn all-articles []
  (db/query-db *db* ["select * from artikler.data"]))

(defn get-article [id]
  (let [[article] (db/query-db *db* ["select * from artikler.data where id = ?" id])]
    (if (seq article) article)))

(defn tag-article [article]
  (tag/tag-text article))

(defn retag-article [id]
  (do (db/delete-from-db *db* "artikler.tagged" ["id=?" (sha (:body (get-article id)))])
      (tag-article (get-article id))))

(defn delete-cached [id]
  (db/delete-from-db *db* "artikler.tagged" ["id=?" (sha (:body (get-article id)))]))

(defn get-tagged-article [id]
  (-> id (get-article) (tag-article)))

(defn query-ssr-boolean [location]
  (db/query-db *db* ["SELECT * FROM ssr.stedsnavn WHERE upper(name) = ?"
                     (str/upper-case location)]))

(defn query-words [tagged]
  (let [queried (map query-ssr-boolean (map :word tagged))
        result? (map #(if (seq %) true false) queried)]
    (remove empty? (map #(if %1 (conj %2 {:locations %3})) result? tagged queried))))

(defn pprint-tagged [tagged]
  (apply str (map #(str % " ") (map :word tagged))))

(defn first-words [tagged]
  (map first (obt/split-sentences tagged)))
