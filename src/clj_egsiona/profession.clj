(ns clj-egsiona.profession
  (:use [ogrim.common.tools]))

(def ^{:dynamic true :private true} *specific*
  ["operasjonsleder" "advokat" "prosjekteringsleder"])

(def ^{:dynamic true :private true} *suffixes*
  ["leder" "lege" "pleier" "betjent" "offiser" "sjef" "rådgiver" "direktør"
   "person"])

(def ^{:dynamic true :private true} *p-suffixes*
  (map #(re-pattern (str ".+" % "\\Z")) *suffixes*))

(def ^{:dynamic true :private true} *p-specific*
  (map #(re-pattern (str "\\b" % "\\b")) *specific*))

(defn- match-word [patterns word]
  (->> patterns
       (map #(re-seq % word))
       (some seq)))

(defn possible-profession? [s]
  (if (match-word *p-suffixes* (.toLowerCase s)) true false))

(defn specific-profession? [s]
  (if (match-word *p-specific* (.toLowerCase s)) true false))
