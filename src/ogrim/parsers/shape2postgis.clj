(ns ogrim.parsers.shape2postgis
  (:import (java.io File))
  (:require [clojure.string :as str]))

(def n-end "_Kartdata")
(def N50 (str "N50" n-end))
(def N250 (str "N250" n-end))
(def N500 (str "N500" n-end))

(def db-name "norge-digitalt")
(def db-host "localhost")
(def db-port "5432")
(def db-user "postgres")

(def shape-srid "32632")
(def shape-encoding "LATIN1")

(defn map-path [format]
  (str "/home/ogrim/data/N50-N5000_Kartdata/" format "/fylke_kommune/12_Hordaland/"))

(defn dir-descendents [dir]
  (file-seq (File. dir)))

(defn walk [dirpath pattern]
  (let [matches (ref '())]
    (doseq [file (-> dirpath File. file-seq)]
      (if (re-matches pattern (.getName file))
        (dosync (alter matches conj (.getPath file)))))
    @matches))

(defn find-siblings [file format]
  (let [filename (last (str/split file #"/"))
        name (.substring filename 0 (- (count filename) 4))]
   (walk (map-path format) (re-pattern (str name ".*")))))

(defn file->extension [file]
  (let [name (last (str/split file #"/"))]
    (last (str/split name #"[.]"))))

(defn siblings->map [files]
  (let [results (hash-map)]
    (apply conj (map #(assoc results (keyword (file->extension %)) %) files))))

(defn file->kommune [file]
  (str \p (nth (reverse (str/split file #"/")) 3)))

(defn file->unique-tablename [file]
  (let [splits (str/split file #"/")
        name (last splits)
        kommune (last (str/split (nth splits 7) #"_"))
        filtered (str/split (first (str/split name #"[.]")) #"_")]
    (apply #(.toLowerCase (str kommune "_" %2 "_" %3)) filtered)))

(defn file->tablename [file]
  (let [name (last (str/split file #"/"))
        filtered (str/split (first (str/split name #"[.]")) #"_")]
    (apply #(.toLowerCase (str %2 "_" %3)) filtered)))


(defn make-cmd [file nformat]
  (str "shp2pgsql -s " shape-srid " -W " shape-encoding " " file " " nformat "." (file->tablename file)
       " | psql -h " db-host " -p " db-port " -d " db-name " -U " db-user))

(defn prepare-cmd [file nformat]
  (str "shp2pgsql -p -s " shape-srid " -W " shape-encoding " " file " " nformat "." (file->tablename file)
       " | psql -h " db-host " -p " db-port " -d " db-name " -U " db-user))

(defn insert-cmd [file nformat]
  (str "shp2pgsql -a -s " shape-srid " -W " shape-encoding " " file " " nformat "." (file->tablename file)
       " | psql -h " db-host " -p " db-port " -d " db-name " -U " db-user))

(defn preparation-script
  "Writes commands needed to create tables to the file in filepath.
  If filepath is \"/home/user/preparation\"
  Execute all written commands by using the following command in bash:
    while read -r line ; do eval $line ; done < preparation"
  [filepath] (spit filepath
                   (str (->> (walk (map-path N50) #".*\.shp")
                             (map #(prepare-cmd % "n50"))
                             (map #(str % "\n"))
                             (apply str))
                        (->> (walk (map-path N250) #".*\.shp")
                             (map #(prepare-cmd % "n250"))
                             (map #(str % "\n"))
                             (apply str))
                        (->> (walk (map-path N500) #".*\.shp")
                             (map #(prepare-cmd % "n500"))
                             (map #(str % "\n"))
                             (apply str)))))

(defn insertion-script
  "Writes commands needed to convert shapefiles to PostGIS data, and insertion commands,
  to the file in filepath. If filepath is \"/home/user/insertion\"
  Execute all written commands by using the following command in bash:
    while read -r line ; do eval $line ; done < insertion"
  [filepath] (spit filepath
                   (str (->> (walk (map-path N50) #".*\.shp")
                             (map #(insert-cmd % "n50"))
                             (map #(str % "\n"))
                             (apply str))
                        (->> (walk (map-path N250) #".*\.shp")
                             (map #(insert-cmd % "n250"))
                             (map #(str % "\n"))
                             (apply str))
                        (->> (walk (map-path N500) #".*\.shp")
                             (map #(insert-cmd % "n500"))
                             (map #(str % "\n"))
                             (apply str)))))

