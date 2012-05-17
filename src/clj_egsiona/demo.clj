(ns clj-egsiona.demo
  (:use [clj-egsiona.core]
        [ogrim.common.tools]
        [clojure pprint repl]
        [clj-egsiona.country]
        [clj-egsiona.geocode])
  (:require [clj-egsiona.evaluation :as ev]))


(def e1 (evaluate-first-half))
(def e2 (evaluate-second-half))
(def e3 (evaluate-all))

(def p (process-id 17))

(keys p)
(:address p)
(:counties p)
(:countries p)
(:regions p)


(map :location (ev/corpus-tags 17))

(map #(map :word %) (let [a (get-tagged-article 17)]
   (disambiguate-names a (possible-location a))))

(process-id 1)
("Åsaneveien" "Sandviken" "Eidsvåg" "Åsane" "Bergen")



(evaluate-first-half) ; med alle lokasjoner
{:precision 25.9, :recall 94.8, :f-measure 39.2}

(evaluate-first-half) ; filtrert med ssr
{:precision 61.1, :recall 78.9, :f-measure 66.3}

(evaluate-second-half) ; filtrert med ssr
{:precision 59.7, :recall 80.5, :f-measure 66.1}


(evaluate-all) ; med alle lokasjoner
"Elapsed time: 51584.259745 msecs"
{:precision 26.1, :recall 95.4, :f-measure 39.4}

(evaluate-all) ; filtrert med ssr
"Elapsed time: 50666.791188 msecs"
{:precision 60.4, :recall 79.7, :f-measure 66.2}



























(comment

(def alle (all-articles))
;(pprint (first alle))

(def ca corpus-articles)


(def all-corpus-tags (map corpus-tags corpus-articles)) ; alle tags fra corpuset
(def all-corpus-articles (map get-article corpus-articles)) ; alle artikler fra corpuset


(def p1 (tag-article (first all-corpus-articles)))
(def p2 (process-locations p1))
(def adresse (first p2))
(def syntaktisk (map :word (second p2)))

(def a1 (map geocode adresse))
(def s1 (query-words (second p2)))
(def s2 (map #(:name (first (:locations %))) s1))

(def c1 (map :location (first all-corpus-tags)))


(def pp1 (tag-article (get-article 17)))
(def pp2 (process-locations pp1))
(def ppc (corpus-tags 17))
(def pp-region (filter #(region? (:word %)) pp1))
(def pp-land (filter #(country? (:word %)) pp1))
(def pp-fylke (filter #(county? (:word %)) pp1))
(def pp-kommune (filter #(municipality? (:word %)) pp1))













(:body (get-article 17))
  (def t1 "Det er ikke mange som vet at For er et sted i Hordaland, som ofte forveksles med preposisjonen for.")
(def t2 "For er et sted i Hordaland, som ofte forveksles med preposisjonen for.")
(query-ssr-boolean "for")
)
