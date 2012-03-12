(ns clj-egsiona.tagger-provider
  (:require [clj-obt.core :as obt]
            [ogrim.common.db :as db]
            [clojure.java.jdbc :as sql]
            [clj-http.client :as client]
            [ring.util.codec :as r])
  (:use [ogrim.common tools downloader]
        [clj-egsiona.db-provider]))

(def ^{:private true} local? (atom nil))
(def ^{:private true} service-url (atom nil))

(defn set-obt-program [location]
  (do (obt/set-obt-path! location)
      (reset! local? true)))

(defn set-obt-service [location]
  (do (reset! service-url (str "http://" location "/text/?data="))
      (reset! local? false)))

(defn process-vector [v] (str \[ (apply str (map #(str \" % \") v)) \]))

(defn service-tag [s]
  (-> (str @service-url (r/url-encode s))
      (client/get {:as "ISO-8859-1"})
      :body
      read-string))

(defn dispatch-tagger [s]
  (cond @local? (obt/obt-tag s)
        (false? @local?) (if (vector? s) (service-tag (process-vector s)) (service-tag s))
        :else (throw (Exception. "OBT is not configured"))))

(def ^{:dynamic true :private true}  *db* (atom (get-db)))

(defn reset-db []
  (reset! *db* (get-db)))

(defn- hash->article [hash]
  (if (nil? @*db*) nil
   (let [[article] (db/query-db @*db* ["SELECT * FROM tagged WHERE id = ?" hash])]
     (:artikkel article))))

(defn- persist-parsed [hash parsed]
  (cond (nil? @*db*) nil
        (empty? (hash->article hash))
        (do (db/insert-row @*db* "tagged" [hash (str parsed)])
            true)))

(defn- tag-text-persistence
  "Provides the extended TaggerProtocol to users"
  [s]
  (if (nil? @*db*) (dispatch-tagger s)
   (let [hash (sha s)
         article (hash->article hash)]
     (if (seq article)
       (read-string article)
       (let [parsed (dispatch-tagger s)]
         (do (persist-parsed hash parsed)
             parsed))))))

(defn- tag-multiple-persistence [strings]
  (let [hashes (map sha strings)
        articles (map hash->article hashes)
        tag-these (filter #(-> (sha %) (hash->article) (string?) (not)) strings)]
    (if (empty? tag-these) (map read-string articles)
     (loop [[current & more :as tag-result] (dispatch-tagger tag-these)
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
