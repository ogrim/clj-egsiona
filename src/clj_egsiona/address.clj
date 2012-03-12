(ns clj-egsiona.address
  (:require [clojure.string :as str]
            [clj-obt.tools :as obt]
            [clj-egsiona.name :as n])
  (:use [ogrim.common.tools]))

(def ^{:dynamic true :private true} *suffixes*
  ["vei" "veien" "vegen" "gata" "gaten" "gate" "stad" "staden"
   "haug" "haugen" "haugene" "dalen" "dal" "plass" "plassen"
   "smauet" "smau" "heia" "heien" "åsen" "vågen" "stredet"
   "stølen" "skogen" "strand" "botn" "faret" "havn" "gården"
   "vika" "viken" "grenda" "boder" "allmenning" "allmenningen"
   "bakken" "voll" "vollen" "neset" "nes" "marken" "marka"
   "kleiva" "kleivane" "skaret" "svingen" "svingene" "holmen"
   "grend" "tunet" "stien" "lia" "brotet" "myra" "myrane" "myren"
   "kollen" "alléen" "allé" "fjellet" "flaten" "stranden" "leitet"
   "lien" "torget" "hagen" "krysset" "tunnelen" "broen" "broa"
   "almenning" "almenningen" "tangen" "våg" "sund"])

(def ^{:dynamic true :private true} *standalone*
  ["veg" "vei" "gate" "plass"])

(def ^{:dynamic true :private true} *special-suffix*
  ["gt"])

(defn- make-pattern [s] (re-pattern (str ".+" s "\\Z")))

(def ^{:dynamic true :private true} *special-case-match*
  (map make-pattern *special-suffix*))

(def ^{:dynamic true :private true} *patterns*
  (map make-pattern *suffixes*))

(defn- match-word [patterns word]
  (->> patterns
       (map #(re-seq % word))
       (some seq)))

(defn european-route? [s]
  (re-find #"\bE[0-9]+\z" s))

(defn maybe-street?
  "Checks word for common street name suffixes, doesn't match the whole word"
  [word]
  (if (seq (match-word *patterns* word)) true false))

(defn maybe-special-street?
  "Matches words in *special-cases* list"
  [word]
  (if (seq (match-word *special-case-match* word)) true false))

(defn maybe-standalone-street?
  "Checks word for common standalone street name, matches the whole word"
  [word]
  (in? *standalone* word))

(defn maybe-address?
  "Matches specific addresses by looking at the last word suffix, and street number"
  [word]
  (let [[num street] (reverse (str/split word #"\s"))]
    (and (num? num) (or (maybe-street? street) (maybe-special-street? street)))))

(defn resolve-current-address [parsed i]
  (let [preceding (->> (obt/preceding-tag parsed "subst" i) (filter #(> (count (:word %)) 1)))
        word (nth parsed i)
        [f s :as more] (->> parsed (drop (inc i)) (take 2))
        num (cond (num? (:word f)) f (and (obt/in? (:tags f) "<punkt>") (num? (:word s))) [f s] :else [])]
    (flatten [preceding word num])))

(defn verb-before [parsed address]
  (let [i (- (:i (first address)) 2)
        before (if (neg? i) false (obt/in? (:tags (nth parsed i)) "verb"))]
    (true? before)))

(defn find-address [parsed]
  (loop [[word & more :as tmp] parsed , i 0 , result []]
    (cond (not word) result

          (<= (count (:word word)) 1)
          (recur more (inc i) result)

          (punctuation? (last (:word word)))
          (recur (conj more (assoc word :word (trim-trailing-punctuation (:word word)))) i result)

          (maybe-street? (:word word))
          (recur more (inc i)
                 (let [address (resolve-current-address parsed i)]
                   (if (n/first-name? (:word (first address)))
                     result (conj result address))))

          (maybe-standalone-street? (:word word))
          (let [address (resolve-current-address parsed i)]
            (cond (= (count address) 1)
                  (recur more (inc i) result)

                  (verb-before parsed address) result
                  :else (recur more (inc i) (conj result address))))

          (maybe-special-street? (:word word))
          (let [address (drop-while #(not (capitalized? (:word %))) (resolve-current-address parsed i))
                n (first more)]
            (recur more (inc i)
                   (cond (empty? address) result
                         (verb-before parsed address) result
                         (= (:word n) ".") (conj result address)
                         :else result)))

          :else (recur more (inc i) result))))

(defn concat-address [address]
  (->> address
       (mapcat #(str (:word %) " "))
       (apply str)
       (.trim)))
