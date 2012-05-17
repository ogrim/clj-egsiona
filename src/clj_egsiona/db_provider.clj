(ns clj-egsiona.db-provider)

(def ^{:dynamic true :private true}
  *db*
  (atom nil))

(defn set-db [db]
  (reset! *db* db))

(defn get-db []
  (if (seq @*db*)
    @*db*
    (throw (Exception. "database settings not set"))))

(set-db
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname  "//localhost:5432/norge-digitalt"
   :user "postgres"
   :password "babbafet"})
