(ns clj-egsiona.name
  (:use [ogrim.common.tools]
        [clj-egsiona.data.name])
  (:require [clojure.string :as str]
            [clj-obt.tools :as obt]))

(defn- make-pattern [s] (re-pattern (str "\\b" s "\\b")))

(def ^{:private true} p-first-name (map make-pattern first-names))

(def ^{:private true} p-surname (map make-pattern surnames))

(defn- string-in? [s re-patterns]
  (let [sl (.toLowerCase s)]
    (if (some string? (map #(re-matches % sl) re-patterns))
      true false)))

(defn surname? [s]
  (let [trimmed (trim-trailing-punctuation s)]
    (if (<= (count trimmed) 1) false
        (string-in? trimmed p-surname))))

(defn- pattern-in [s re-patterns]
  (remove nil? (map #(re-find % (.toLowerCase s)) re-patterns)))

(defn find-surnames [s]
  (pattern-in s p-surname))

(defn composite-name? [s]
  (if (re-seq #"\b[a-zæøå]+[-][a-zæøå]+\b" (.toLowerCase s)) true false))

(defn valid-composite-name? [s]
  (->> (str/split s #"-")
       (map #(string-in? % p-first-name))
       (every? true?)))

(defn first-name? [s]
  (if (composite-name? s)
    (valid-composite-name? s)
    (string-in? s p-first-name)))

(defn find-first-names [s]
  (pattern-in s p-first-name))

(defn middle-name? [s]
  (or (surname? s) (first-name? s) (capitalized? s)))

(defn- resolve-name [sentence tag]
  (let [spool (->> sentence
                   (reverse)
                   (drop-while #(not (obt/identical-tags tag %)))
                   (rest))
        expanded (-> (take-while #(middle-name? (:word %)) spool)
                     (conj tag)
                     (reverse))
        resolved (drop-while #(not (first-name? (:word %))) expanded)]
    (if (empty? resolved) nil resolved)))

(defn disambiguate-names [tagged possible-locations]
  (let [sentences (obt/split-sentences tagged)
        possible-names (filter #(surname? (:word %)) possible-locations)
        relevant-sentences (map #(obt/tag->sentence sentences %) possible-names)]
    (->> (map resolve-name relevant-sentences possible-names) (remove nil?))))

(defn all-first-names [tagged]
  (let [dropfn (fn [x] (drop-while #(not (first-name? (:word %))) x))]
    (loop [[name & more] (dropfn tagged) , acc []]
      (cond (and (seq name) (empty? more)) (conj acc name)
            (empty? more) acc
            (or (in? (:tags name) "adv") (not (capitalized? (:word name)))) (recur (dropfn more) acc)
            :else (recur (dropfn more) (conj acc name))))))
