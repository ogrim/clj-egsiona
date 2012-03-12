(ns ogrim.common.tools
  (:import [java.security MessageDigest])
  (:require [clojure.string :as str]))

(let [m (.getDeclaredMethod clojure.lang.LispReader
                            "matchNumber"
                            (into-array [String]))]
  (.setAccessible m true)
  (defn parse-number [s]
    (.invoke m clojure.lang.LispReader (into-array [s]))))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (if (some #{elm} seq) true false))

(defn- compute-next-row
  "computes the next row using the prev-row current-element and the other seq"
  [prev-row current-element other-seq pred]
  (reduce
    (fn [row [diagonal above other-element]]
      (let [update-val
	     (if (pred other-element current-element)
	       diagonal
	       (inc (min diagonal above (peek row))))]
        (conj row update-val)))
    [(inc (first prev-row))]
    (map vector prev-row (next prev-row) other-seq)))

(defn- levenshtein-distance-implementation
  [a b & {p :predicate  :or {p =}}]
  (peek (reduce (fn [prev-row current-element]
                  (compute-next-row prev-row current-element b p))
                (map #(identity %2) (cons nil b) (range)) a)))

(def levenshtein-distance (memoize levenshtein-distance-implementation))

(defn capitalized? [[s]]
  (if (re-seq #"[A-ZÆØÅ]" (str s))
    true false))

(defn all-caps? [s]
  (if (= (str/upper-case s) s) true false))

(defn trim-trailing-punctuation [s]
  (first (str/split s #"[.,:;!?]+\Z")))

(defn punctuation? [c]
  (in? #{\. \, \: \; \! \? } c))

(defn num?
  "Checks if string is an integer by trying to parse it"
  [s]
  (try (do (Integer/parseInt s) true)
       (catch Exception _ false)))

(defn truncate-double [double n]
  (->> double (format (str "%." n "f")) (Double/parseDouble)))

(defn percentage [numerator denominator]
  (-> (* (/ numerator denominator) 100) (truncate-double 1)))

(defn sha [input]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (. md update (.getBytes input))
    (let [digest (.digest md)]
      (apply str (map #(format "%02x"  (bit-and % 0xff)) digest)))))
