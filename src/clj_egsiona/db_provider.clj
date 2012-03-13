(ns clj-egsiona.db-provider
  (:require [clojure.java.jdbc :as sql]))

(def ^{:dynamic true :private true}
  *db*
  (atom nil))

(defn set-db [db]
  (reset! *db* db))

(comment (defn get-db []
   (if (seq @*db*)
     @*db*
     (throw (Exception. "database settings not set")))))

(defn get-db [] @*db*)

(defn create-tables []
  (try (sql/with-connection @*db*
         (sql/create-table "tagged"
                           [:id :text "PRIMARY KEY"]
                           [:artikkel :text]))
       (catch Exception e (println e))))
