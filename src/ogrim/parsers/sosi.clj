(ns ogrim.parsers.sosi
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [ogrim.common.db :as db])
  (:use [ogrim.common.tools]))

(def ^:dynamic *db*
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname  "//localhost:5432/norge-digitalt"
   :user "postgres"
   :password ""})

(defn create-table []
  (try (sql/with-connection *db*
         (sql/create-table "ssr.stedsnavn"
                           [:id :serial "PRIMARY KEY"]
                           [:name :text]
                           [:northing "bigint"]
                           [:easting "bigint"]
                           [:kommune "int"]))
       (catch Exception e (println e))))

(defn level? [s]
  (count (first (str/split s #"[A-Z]+"))))

(defn node-name [node]
  (first (str/split (apply str (drop (level? node) node)) #"\s")))

(defn valid-node? [node]
  (let [names (map node-name node)]
    (and (in? names "SNAVN")
         (in? names "KOMM")
         (in? names "NØ"))))

(defn parse-kommune [node]
  (-> (drop-while #(not (= (node-name %) "KOMM")) node)
      (first)
      (str/split #"\ ")
      (second)
      (parse-number)))

(defn parse-name [node]
  (->> (-> (drop-while #(not (= (node-name %) "SNAVN")) node)
           (first)
           (str/split #"\"")
           (rest))
       (interpose " ")
       (apply str)
       (.trim)))

(defn parse-coordinates [node]
  (map parse-number
       (-> (drop-while #(not (= (node-name %) "NØ")) node)
           (second)
           (str/split #"\s"))))

(defn parse-node [node]
  (if (valid-node? node)
    (let [kommunenr (parse-kommune node)
          name (parse-name node)
          [north east] (parse-coordinates node)]
      [name north east kommunenr])))

(defn add-id [parsed]
  (let [id (ref 0)
        result (ref [])]
    (doseq [p parsed]
      (->> (cons (alter id inc) p)
           (vec)
           (conj @result)
           (ref-set result)
           (dosync)))
    @result))

(defn parse-file [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (let [in-node (ref false)
          node (ref '())
          result (ref '())]
      (do (doseq [line (line-seq rdr)]
            (cond
             (and (not @in-node) (seq @node))
             (dosync (let [p (parse-node (reverse @node))]
                       (if (seq p) (ref-set result (cons p @result))))
                     (ref-set node '()))

             (and (not @in-node) (= (node-name line) "TEKST"))
             (dosync (ref-set in-node true) (ref-set node (cons line @node)))

             (and @in-node (= (node-name line) "TEKST"))
             (dosync (let [p (parse-node (reverse @node))]
                       (if (seq p) (ref-set result (cons p @result))))
                     (ref-set node (list line)))

             @in-node
             (dosync (ref-set node (cons line @node)))))

          (dosync (let [p (parse-node (reverse @node))]
                    (if (seq p) (ref-set result (cons p @result)))))

          (add-id (distinct @result))))))

(comment
  (time (db/insert-all-rows *db* "ssr.stedsnavn" (parse-file "path to sosi file")))
                                        ; took about 21 minutes to insert all 94k locations
                                        ; parsing the whole file takes about 2.8 minutes, insertion is the bottleneck
  (count (parse-file "/home/ogrim/data/NDData973/SSR/Uttak/12_Hordaland/UTM33_Euref89/SOSI/hordaland-utf8.sos"))
  )



(defn clean-up []
  (let [all (db/query-db *db* [(str "SELECT name FROM ssr.stedsnavn")])
        d (distinct all)
        call (count all)
        dall (count d)]
    (str "alle:" call " distinct:" dall " difference:" (- call dall))))

;; (def all (db/query-db *db* [(str "SELECT * FROM ssr.stedsnavn ORDER BY name")]))
;; (count (distinct (db/query-db *db* [(str "SELECT name FROM ssr.stedsnavn ORDER BY name")])))
;; (count (filter-locations-fast (db/query-db *db* [(str "SELECT * FROM ssr.stedsnavn ORDER BY name")])))

(defn compare-locations [a b]
  (and (= (:name a) (:name b))
       (= (:kommune a) (:kommune b))
        (not= (:id a) (:id b))))

(defn location-in? [loc coll]
  (if (some true? (map #(compare-locations loc %) coll))
    true false))

(defn filter-locations [[head & locations]]
  (loop [[loc & more] locations, current head, acc []]
    (cond (empty? loc) (conj acc current)
          (compare-locations current loc) (recur more current acc)
          :else (recur more loc (conj acc current)))))

(defn group-locations [[head & locations]]
  (loop [[loc & more] locations, current [head], acc []]
    (cond (empty? loc) (conj acc current)
          (compare-locations (first current) loc) (recur more (conj current loc) acc)
          :else (recur more [loc] (conj acc current)))))

(defn st_geomfromtext [loc]
  (str "ST_GeomFromText('POINT(" (:easting loc) " " (:northing loc) ")',32633)"))

(defn distance
  "Measures distance in meters between two locations"
  [from to]
  (->> [(str "SELECT ST_Distance (" (st_geomfromtext from) ", " (st_geomfromtext to) ");")]
       (db/query-db *db*)
       (first)
       (:st_distance)))

(defn distance-id [id-from id-to]
  (let [[from to] (db/query-db *db* [(str "SELECT * FROM ssr.stedsnavn WHERE id = " id-from " OR id = " id-to)])]
    (distance from to)))

;; (defn distance-filter [meter-cutoff location locations]
;;   (loop [[current & more] locations, acc [location]]
;;     (cond (empty? current) acc
;;           (> (distance location current) meter-cutoff) (recur more (conj acc current))
;;           :else (recur more acc))))

(defn distance-filter? [meter-cutoff from to]
  (if (< (distance from to) meter-cutoff)
    true false))

(defn distance-filter-all [meter-cutoff [location & locations]]
  (loop [[current & more] locations
         acc [location]]
    (cond (empty? current) acc
          (some false? (map #(distance-filter? meter-cutoff current %) acc))
          (recur more (conj acc current))
          :else (recur more acc))))

(defn format-locations [locations]
  (let [i (ref 0), result (ref [])]
    (do (doseq [l locations]
          (dosync (alter result conj [(alter i inc) (:name l) (:northing l) (:easting l) (:kommune l)])))
        @result)))

(defn process-group [meter-cutoff locations]
  (if (= (count locations) 1) locations
      (distance-filter-all meter-cutoff locations)))

(defn process-duplicates []
  (->> (db/query-db *db* [(str "SELECT * FROM ssr.stedsnavn ORDER BY name")])
       (take 1000)
       (group-locations)
       (map #(process-group 1000 %))
       (flatten)
       (format-locations)))

(defn process-duplicates-cheap []
  (->> (db/query-db *db* [(str "SELECT * FROM ssr.stedsnavn ORDER BY name")])
       (group-locations)
       (map first)
       (flatten)
       (format-locations)))

(comment
  (time (db/insert-all-rows *db* "ssr.data" (process-duplicates-cheap))))

;; (def bergen (db/query-db *db* [(str "SELECT * FROM ssr.stedsnavn WHERE name = 'Bergen'")]))
