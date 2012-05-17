(ns ogrim.parsers.geonames
  (:require [clojure.string :as str])
  (:require [clojure.java.jdbc :as sql]))

(def geonames-path "path-to-geonames-file")

(def *db*
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname  "//localhost:5432/norge-digitalt"
   :user "postgres"
   :password ""})

(defn line->map [line]
  (apply #(conj {:geonameid (Integer/parseInt %1)}
                {:name %2}
                {:asciiname %3}
                {:altnames %4}
                {:latitude (Double/parseDouble %5)}
                {:longitude (Double/parseDouble %6)}
                {:fclass %7}
                {:fcode %8}
                {:ccode %9}
                {:ccode2 %10}
                {:admin1 %11}
                {:admin2 %12}
                {:admin3 %13}
                {:admin4 %14}
                {:population %15}
                {:elevation %16}
                {:gtopo %17}
                {:timezone %18}
                {:date %19}) (str/split line #"\t")))

(defn create-table []
  (try (sql/with-connection *db*
         (sql/create-table "geonames.data"
                           [:geonameid "integer PRIMARY KEY"]
                           [:name :text]
                           [:asciiname :text]
                           [:altnames :text]
                           [:latitude "double precision"]
                           [:longitude "double precision"]
                           [:fclass :text]
                           [:fcode :text] 
                           [:ccode :text]
                           [:ccode2 :text]
                           [:admin1 :text]
                           [:admin2 :text]
                           [:admin3 :text]
                           [:admin4 :text]
                           [:population :text]
                           [:elevation :text]
                           [:gtopo :text]
                           [:timezone :text]
                           [:date :text]))
       (catch Exception e (println e))))

;; (defn insert-line [line]
;;   (sql/with-connection *db*
;;     (sql/insert-records "geonames.data" (line->map line))))

;; (defn insert-all [geonames]
;;   (with-open [rdr (clojure.java.io/reader geonames)]
;;     (doseq [line (line-seq rdr)] (insert-line line))))

(defn insert-all [geonames]
  "Inserts all lines in geonames file into *db*"
  (sql/with-connection *db*
    (with-open [rdr (clojure.java.io/reader geonames)]
     (doseq [line (line-seq rdr)]
       (sql/insert-records "geonames.data" (line->map line))))))
