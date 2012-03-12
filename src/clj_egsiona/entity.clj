(ns clj-egsiona.entity
  (:use [ogrim.common.tools]))

(def ^{:private true} han
  ["mann" "avtale" "konvensjon" "restaurant" "skoleplass" "byggeplass" "barnehage"
   "forening" "klokke" "klinikk" "familie" "stasjon" "parkeringsplass" "holdeplass"])

(def ^{:private true} intet
  ["direktorat" "senter" "firma" "forbund" "område" "departetment" "tilsyn"
   "anlegg" "verk"])

(defn- intet->best [s]
  (if (= (last s) \e) (str s \t) (str s "et")))

(defn- han->best [s]
  (if (= (last s) \e) (str s \n) (str s "en")))

(defn- make-pattern [s] (re-pattern (str ".*" s "\\Z")))

(def ^{:private true} p-han
  (->> (mapcat (fn [x] [x (han->best x)]) han)
       (map make-pattern)))

(def ^{:private true} p-intet
  (->> (mapcat (fn [x] [x (intet->best x)]) intet)
       (map make-pattern)))

(defn- match-word [patterns word]
  (->> patterns
       (map #(re-seq % word))
       (some seq)))

(defn maybe-entity? [s]
  (if (or (seq (match-word p-han s))
          (seq (match-word p-intet s))) true false))
