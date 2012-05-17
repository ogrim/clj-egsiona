(ns clj-egsiona.evaluation
  (:require [ogrim.common.db :as db]
            [clj-egsiona.core :as core]
            [clj-egsiona.article :as a])
  (:use [clj-egsiona db-provider name]
        [ogrim.common.tools]))

(def corpus-articles
  [1 2 4 7 8 10 12 14 15 16 17 18 19 20 22 23 24 28 31 32 33 34 36 37 38
   39 40 43 44 45 47 48 50 51 52 57 58 59 60 61 62 63 64 66 67 69 70 71
   77 78 79 80 81 82 83 84 85 86 87 89 91 94 97 99 100 104 108 110 111
   112 114 116 118 122 125 127 132 134 136 138 139 141 144 149 150 152
   156 157 158 161 164 165 168 169 171 172 180 182 184 188 190 191 193
   194 198 199 201 203 204 206 208 210 243])

(def under-300
  [1 2 4 10 14 15 16 17 19 22 24 28 31 33 36 38 39 40 43 44 45 50 51 52
   58 59 60 61 63 67 70 71 78 79 80 82 83 84 86 87 89 91 99 111 112 114
   116 122 125 127 132 134 136 139 150 152 156 157 165 168 171 172 182
   190 191 199 204 206 243])

(def quads (partition-all (/ (count corpus-articles) 4) corpus-articles))

(defn select-articles-by-words [num]
  (filter #(< (:i (last (a/get-tagged-article %))) num) corpus-articles))

(def ^:dynamic *db* (get-db))

(def problematic
  #{(nth corpus-articles 21)
    (nth corpus-articles 30)
    (nth corpus-articles 35)
    (nth corpus-articles 40)
    (nth corpus-articles 42)
    (nth corpus-articles 83)
    (nth corpus-articles 93)
    (nth corpus-articles 99)
    })

(defn corpus-tags [id]
  (db/query-db *db* ["select * from artikler.corpus where artikkel = ?" id]))

(defn precision
  "the fraction of retrieved instances that are relevant"
  [tp fp]
  (let [denominator (+ tp fp)]
    (if (zero? denominator) 0
        (/ tp denominator))))

(defn recall
  "the fraction of relevant instances that are retrieved"
  [tp fn]
  (let [denominator (+ tp fn)]
    (if (zero? denominator) 0 (/ tp denominator))))

(defn f-measure [precision recall]
  (let [denominator (+ precision recall)]
    (if (zero? denominator) 0
        (/ (* 2 precision recall) denominator))))

(defn report-metrics [processed corpus-tags]
  (let [corpus (map #(.toLowerCase (:location %)) corpus-tags)
        {a :address l :locations c :counties country :countries r :regions e :eu-route g :grammar} processed
        locations (->> (map #(.toLowerCase (:word %)) l)
                       (map trim-trailing-punctuation))
        address (->> (map #(.toLowerCase %) a)
                     (map trim-trailing-punctuation))
        grammar (->> (map #(.toLowerCase (:word %)) g)
                     (map trim-trailing-punctuation))
        all (distinct (concat locations address c country r e grammar))
        tp (filter #(in? all %) corpus)
        fn (filter #(not (in? all %)) corpus)
        fp (filter #(not (in? corpus %)) all)]
    {:truepositive tp :falsenegative fn :falsepositive fp}))

(defn calculate-metrics [processed corpus-tags]
  (let [{tn :truepositive fn :falsenegative fp :falsepositive} (report-metrics processed corpus-tags)]
    [(count tn) (count fn) (count fp)]))

(defn evaluate [processed corpus-tags]
  (let [[tp fn fp] (calculate-metrics processed corpus-tags)
        p (double (precision tp fp))
        r (double (recall tp fn))]
    {:precision p
     :recall r
     :f-measure (f-measure p r)}))

(defn evaluation-report [evaluation]
  (let [recall (sort (map :recall evaluation))
        p-recall (percentage (reduce + recall) (count recall))
        precision (sort (map :precision evaluation))
        p-precision (percentage (reduce + precision) (count precision))
        f-measure (map :f-measure evaluation)]
    {:precision p-precision
     :recall p-recall
     :f-measure (percentage (reduce + f-measure) (count f-measure))}))

(defn pretty-corpus [id]
  (map #(.toLowerCase (:location %)) (corpus-tags id)))

(defn pretty-locations [id]
 (let [t (corpus-tags id)
       {a :address loc :locations c :counties co :countries} (core/process-id id)
       pm #(.toLowerCase %)
       wm #(.toLowerCase (:word %))]
   (-> [(map pm a) (map wm loc) (map pm c) (map pm co)] (flatten) (distinct))))

(defn report-metrics-id [id]
  (report-metrics (core/process-id id) (corpus-tags id)))

(defn evaluate-corpus [corpus]
  (->> corpus (map #(evaluate (core/process-id %) (corpus-tags %))) (evaluation-report)))

(defn map-calculate-metrics [corpus]
  (->> corpus (map #(calculate-metrics (core/process-id %) (corpus-tags %)))))

(defn evaluate-all []
  (evaluate-corpus corpus-articles))

(defn evaluate-first-half []
  (evaluate-corpus (-> (count corpus-articles) (/ 2) (take corpus-articles))))

(defn evaluate-second-half []
  (evaluate-corpus (-> (count corpus-articles) (/ 2) (drop corpus-articles))))

(comment (require '(clj-egsiona [core :as core]) :reload))
