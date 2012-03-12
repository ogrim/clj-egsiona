(ns clj-egsiona.processing
  (:require [clj-obt.tools :as obt]
            [clj-egsiona.tagger-provider :as tag]
            [clojure.string :as str])
  (:use [ogrim.common.tools]
        [clj-egsiona address country name
         profession domain entity]))

(defn possible-address [tagged]
  (->> (find-address tagged)
       (filter #(or (not= (last (:word (last %))) \s) (not (obt/in? (:tags (last %)) "gen"))))))

(defn disambiguate-address [address counties professions]
  (let [patterns (map #(re-pattern (.toLowerCase %)) address)
        filterfn (fn [c] (remove nil? (map #(if (re-find %1 c) %2) patterns address)))
        result (mapcat filterfn counties)
        profession-fn (fn [x] (every? nil? (map #(re-find (re-pattern (str (.toLowerCase (:word %)) "\\b")) x) professions)))
        validation-fn (fn [x] (or (maybe-street? x) (maybe-address? x) (maybe-standalone-street? (last (str/split x #"[ ]")))))]
    (->> address
         (filter #(not (in? result %)))
         (mapcat #(str/split % #"/"))
         (filter profession-fn)
         (filter validation-fn)
         (filter #(not (maybe-entity? %))))))

(defn grammar-locations [tagged]
  (let [prepositions (obt/filter-tag tagged "prep")]
   (->> (obt/drop-tag prepositions "verb")
        (map #(obt/next-tag tagged %))
        (filter #(> (count (:word %)) 1))
        (filter #(not (num? (:word %))))
        (filter #(not (all-caps? (:word %))))
        (filter #(capitalized? (:word %)))
        (filter #(not (in? (:tags (obt/next-tag tagged %)) "verb"))))))

(defn disambiguate-grammar [tagged grammar]
  (->> (all-first-names grammar)
       (reduce obt/remove-tag grammar)
       (filter #(not (possible-domain? (:word %))))
       (filter #(not (maybe-entity? (:word %))))
       (filter #(not (and (= (last (:word %)) \s) (in? (:tags %) "gen"))))))

(defn process-locations [s]
  (let [tagged (tag/tag-text s)
        address (map concat-address (possible-address tagged))
        counties (find-counties s)
        professions (filter #(possible-profession? (:word %)) tagged)
        eu-route (filter #(european-route? (:word %)) tagged)
        grammar (grammar-locations tagged)]
    {:address (disambiguate-address address counties professions)
     :counties counties
     :countries (find-countries s)
     :regions (find-regions s)
     :eu-route (map #(.toLowerCase (:word %)) eu-route)
     :grammar (disambiguate-grammar tagged grammar)
     }))

(defn process-text [s]
  (let [{a :address l :locations c :counties country :countries r
         :regions e :eu-route g :grammar} (process-locations s)
         address (->> (map #(.toLowerCase %) a)
                      (map trim-trailing-punctuation))
         grammar (->> (map #(.toLowerCase (:word %)) g)
                      (map trim-trailing-punctuation))]
    (distinct (concat address c country r e grammar))))
