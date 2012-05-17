(ns clj-egsiona.corpus
  (:require [ogrim.common.db :as db]
            [clojure.string :as str]
            [clojure.java.jdbc :as sql]))

(def ^:dynamic *db*
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname  "//localhost:5432/norge-digitalt"
   :user "postgres"
   :password ""})

(defn all-articles []
  (db/query-db *db* ["select * from artikler.data"]))

(defn create-table []
  (try (sql/with-connection *db*
         (sql/create-table "artikler.corpus"
                           [:id :serial "PRIMARY KEY"]
                           [:artikkel :serial]
                           [:location :text]))
       (catch Exception e (println e))))

(def id (atom 0))
(defn next-id []
  (swap! id inc))

(def article-id (atom -1))

(defn next-article []
  (:body (nth alle (swap! article-id inc))))
(defn reprint []
  (:body (nth alle @article-id)))
(defn insert [location]
  (try (db/insert-row *db* "artikler.corpus" [(next-id) (:id (nth alle @article-id)) location])
       (catch Exception e (do (swap! id dec) (throw e)))))


(comment
(insert "Haukeland")
(insert "Hordaland")
(insert "Bergen")

(insert "")

(geocode "")
(def c (db/query-db *db* ["select * from artikler.corpus"]))

(spit "/home/ogrim/data/all-articles" alle)
  )
