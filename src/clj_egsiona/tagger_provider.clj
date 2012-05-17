(ns clj-egsiona.tagger-provider
  (:require [clj-obt.core :as obt]
            [ogrim.common.db :as db]
            [clojure.java.jdbc :as sql])
  (:use [ogrim.common.tools]
        [clj-egsiona.db-provider]))

(obt/set-obt-path! "/home/ogrim/bin/The-Oslo-Bergen-Tagger")

(def ^{:dynamic true :private true}  *db* (get-db))

(defn- hash->article [hash]
  (let [[article] (db/query-db *db* ["SELECT * FROM artikler.tagged WHERE id = ?" hash])]
    (:artikkel article)))

(defn- persist-parsed [hash parsed]
  (if (empty? (hash->article hash))
    (do (db/insert-row *db* "artikler.tagged" [hash (str parsed)])
        true)))

(defn- tag-text-persistence
  "Provides the extended TaggerProtocol to users"
  [s]
  (let [hash (sha s)
        article (hash->article hash)]
    (if (seq article)
      (read-string article)
      (let [parsed (obt/obt-tag s)]
        (do (persist-parsed hash parsed)
            parsed)))))

(defn- tag-multiple-persistence [strings]
  (let [hashes (map sha strings)
        articles (map hash->article hashes)
        tag-these (filter #(-> (sha %) (hash->article) (string?) (not)) strings)]
    (if (empty? tag-these) (map read-string articles)
     (loop [[current & more :as tag-result] (obt/obt-tag tag-these)
            [tagged & tmore] articles
            [hash & hmore] hashes
            acc []]
       (cond (string? tagged) (recur tag-result tmore hmore (conj acc (read-string tagged)))
             (empty? tmore) (if (empty? current) acc (conj acc (do (persist-parsed hash current) current)))
             :else (recur more tmore hmore (conj acc (do (persist-parsed hash current) current))))))))

(defn tag-multiple-dispatcher [content]
  (let [contenttype (map type content)]
    (cond (every? true? (map #(= java.lang.String %) contenttype)) (tag-multiple-persistence content)
          (every? true? (map #(= clojure.lang.PersistentStructMap %) contenttype)) (tag-multiple-persistence (map :body content))
          :else (throw (IllegalArgumentException. "make sure you only tag either Strings or valid article maps (with :body key)")))))

(defprotocol TaggerProtocol
  (tag-text [s]))

(extend-protocol TaggerProtocol
  java.lang.String
  (tag-text [s]
    (tag-text-persistence s))
  clojure.lang.IPersistentMap
  (tag-text [s]
    (tag-text-persistence (:body s)))
  clojure.lang.IPersistentVector
  (tag-text [s]
    (tag-multiple-dispatcher s))
  clojure.lang.LazySeq
  (tag-text [s]
    (tag-multiple-dispatcher s)))

(defn- create-table []
  (try (sql/with-connection *db*
         (sql/create-table "artikler.tagged"
                           [:id :text "PRIMARY KEY"]
                           [:artikkel :text]))
       (catch Exception e (println e))))
