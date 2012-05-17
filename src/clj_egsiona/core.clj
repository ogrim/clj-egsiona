(ns clj-egsiona.core
  (:require [clj-obt.tools :as obt]
            [clj-egsiona.geocode :as geo]
            [clojure.string :as str])
  (:use [ogrim.common.tools]
        [clj-egsiona address country name
         profession article domain entity]))

(defn possible-location [tagged]
  (-> tagged
      (obt/drop-tags unvanted-tags)
      (obt/filter-capitalized)
      (obt/distinct-tags)))

(defn disambiguate-locations
  "Disambiguates possible locations"
  [tagged locations counties first-words]
  (let [ssr-filtered (filter #(seq? (query-ssr-boolean (:word %))) locations)
        not-ssr (filter #(not (in? ssr-filtered %)) locations)
        surnames (disambiguate-names tagged locations)
        names (mapcat #(map :word %) surnames)
        names-without-punctuation (map trim-trailing-punctuation names)
        first-words-mapped (map :word first-words)

        ;;TODO use counties to check not-ssr with geocoder
        ]
    (concat (-> ssr-filtered
                (obt/drop-words names)
                (obt/drop-words names-without-punctuation)
                (obt/drop-words first-words-mapped))

            (comment (-> not-ssr
                 (obt/drop-words names)
                 (obt/drop-words names-without-punctuation)
                 (obt/drop-words first-words-mapped)))

            )))

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
        (filter #(not (in? (:tags (obt/next-tag tagged %)) "verb")))
)))

(defn disambiguate-grammar [tagged locations]
  (->> (all-first-names locations)
       (reduce obt/remove-tag locations)
       (filter #(not (possible-domain? (:word %))))
       (filter #(not (maybe-entity? (:word %))))
       (filter #(not (and (= (last (:word %)) \s) (in? (:tags %) "gen"))))
       ))

;{:truepositive ("nygårdsparken" "bergen"),
;:falsenegative (),
;:falsepositive ("grasdal" "synes" "julia" "institutt" "universitetet" "statistisk")}

(defn process-locations [s]
  (let [tagged (tag-article s)
;        first-names ;(all-first-names tagged)
        first-words (first-words tagged)
        address (map concat-address (possible-address tagged))
        counties (find-counties s)
        countries (find-countries s)
        locations (possible-location tagged)
        professions (filter #(possible-profession? (:word %)) tagged)
        eu-route (filter #(european-route? (:word %)) locations)
        grammar (grammar-locations tagged)]
    {:address (disambiguate-address address counties professions)
     :locations [];(disambiguate-locations tagged locations counties first-words)
     :counties counties
     :countries countries
     :regions (find-regions s)
     :eu-route (map #(.toLowerCase (:word %)) eu-route)
     :grammar (disambiguate-grammar tagged grammar)
     :all locations}))

(defn process-id [id]
  (process-locations (:body (get-article id))))

(comment

  (defn test-location-d [id]
  (let [s (:body (get-article id))
        t (get-tagged-article id)
        f (first-words t)
        l (possible-location t)
        c (find-counties s)]
    (disambiguate-locations t l c f)))

(use '(clj-egsiona.name) :reload)
(require '(clj-egsiona [tagger-provider :as tag]) :reload)
)
