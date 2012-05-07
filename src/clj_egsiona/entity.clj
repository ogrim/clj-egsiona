(ns clj-egsiona.entity
  (:use [ogrim.common.tools]))

(def ^{:private true} vokaler
  #{\a \e \i \o \u})

(defn- konsonant? [c]
  (not (in? vokaler c)))

(def ^{:private true} irregular
  ["mann" "mannen" "menn" "mennene"
   "senter" "senteret" "setre" "sentrene"])

(def ^{:private true} han
  ["avtale" "konvensjon" "restaurant" "skoleplass" "byggeplass" "barnehage"
   "forening" "klokke" "klinikk" "familie" "stasjon" "parkeringsplass" "holdeplass"])

(def ^{:private true} intet
  ["direktorat" "firma" "forbund" "område" "departetment" "tilsyn"
   "anlegg" "verk"])

(defn- han->best [s]
  (if (= (last s) \e) (str s \n) (str s "en")))

(defn- han->ubest-fl [s]
  (if (= (last s) \e) (str s \r) (str s "er")))

(defn- han->best-fl [s]
  (if (= (last s) \e) (str s "ne") (str s "ene")))

(defn- intet->best [s]
  (if (= (last s) \e) (str s \t) (str s "et")))

(defn- intet->ubest-fl [s]
  (cond (konsonant? (last s)) s
        (= (last s) \e) (str s \r)
        :else (str s "er")))

(defn- intet->best-fl [s]
  (if (= (last s) \e) (str s "ne") (str s "ene")))

(defn- make-pattern [s] (re-pattern (str ".*" s "\\Z")))

(defn- expand-han [word]
  [word (han->best word) (han->ubest-fl word) (han->best-fl word)])

(defn- expand-intet [word]
  [word (intet->best word) (intet->ubest-fl word) (intet->best-fl word)])

(def ^{:private true} p-han
  (->> (mapcat expand-han han)
       (map make-pattern)))

(def ^{:private true} p-intet
  (->> (mapcat expand-intet intet)
       (map make-pattern)))

(def ^{:private true} p-irregular
  (map make-pattern irregular))

(defn- match-word [patterns word]
  (->> patterns
       (map #(re-seq % word))
       (some seq)))

(defn maybe-entity? [s]
  (if (or (seq (match-word p-han s))
          (seq (match-word p-intet s))
          (seq (match-word p-irregular s))) true false))
