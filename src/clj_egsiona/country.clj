(ns clj-egsiona.country
  (:use [ogrim.common.tools]
        [clj-egsiona.data.country])
  (:require [clj-obt.tools :as obt]
            [clojure.string :as str]))

(def ^{:private true} p-fn (fn [x] (re-pattern (str "[ ,.]" x "[ ,.:;!?]"))))

(def ^{:private true} p-countries (map p-fn countries))
(def ^{:private true} p-continents (map p-fn continents))
(def ^{:private true} p-regions (map p-fn regions))
(def ^{:private true} p-counties (map p-fn counties))
(def ^{:private true} p-municipalities (map p-fn municipalities))
(def ^{:private true} p-direction (map p-fn directions))

(defn- pattern-in [s re-patterns]
  (->> re-patterns
       (map #(re-find % (.toLowerCase s)))
       (remove nil?)
       (map #(.trim %))
       (map trim-trailing-punctuation)))

(defn find-countries [s]
  (pattern-in s p-countries))

(defn find-continents [s]
  (pattern-in s p-continents))

(defn find-regions [s]
  (pattern-in s p-regions))

(defn find-counties [s]
  (pattern-in s p-counties))

(defn find-municipalities [s]
  (pattern-in s p-municipalities))

(defn find-directions [s]
  (pattern-in s p-direction))

(defn- string-in? [s re-patterns]
  (let [sl (.toLowerCase s)]
    (if (some string? (map #(re-matches % sl) re-patterns))
      true false)))

(defn country? [s]
  (string-in? s p-countries))

(defn continent? [s]
  (string-in? s p-continents))

(defn region? [s]
  (string-in? s p-regions))

(defn county? [s]
  (string-in? s p-counties))

(defn municipality? [s]
  (string-in? s p-municipalities))

(defn- county->tags
  "Takes the county split by #\"[ ]\" and find all occurences in tagged"
  [tagged county-split]
  (let [spoolfn (fn [ttc] (drop-while #(not= (:i %) (:i ttc)) tagged))
        filterfn (fn [spool] (let [r (map #(if (= (.toLowerCase (:word %1)) (.toLowerCase %2)) %1) spool county-split)]
                              (if (every? false? (map nil? r)) r)))]
    (->> (first county-split) (obt/filter-word-insensitive tagged) (map spoolfn) (map filterfn) (remove nil?))))

(defn resolve-counties
  "Resolves all occurences of the counties in tagged, use (find-counties)"
  [tagged counties]
  (let [{single true , multi false :as lol}
        (->> counties
             (map #(str/split % #"[ ]"))
             (group-by #(= (count %) 1)))]
    (concat (map #(obt/filter-word-insensitive tagged (first %)) single)
            (map #(county->tags tagged %) multi))))
