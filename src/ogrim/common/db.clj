(ns ogrim.common.db
  (:require [clojure.java.jdbc :as sql]))

(defn query-db [db query]
  (sql/with-connection db
    (sql/with-query-results rs query
      (doall rs))))

(defn delete-from-db [db table where]
  (sql/with-connection db
    (sql/delete-rows table where)))

(defn doto-rows
  "Applies the function on all the rows returned from query:
    (function row)"
  [db query function]
  (sql/with-connection db
    (sql/with-query-results rs [query]
      (doseq [row rs] (function row)))))

(defn insert-row [db table row]
  (sql/with-connection db
    (sql/insert-rows table row)))

(defn insert-all-rows [db table rows]
  (sql/with-connection db
    (doseq [row rows]
      (sql/insert-rows table row))))
