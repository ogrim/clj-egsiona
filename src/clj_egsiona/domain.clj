(ns clj-egsiona.domain
  (:use [ogrim.common.tools]))

(def ^{:private true} tld
  ["no" "com" "net" "org" "se" "dk" "de"])

(def ^{:private true} p-tld
  (map #(re-pattern (str ".+[.]" % "\\Z")) tld))

(defn- match-word [patterns word]
  (->> patterns
       (map #(re-seq % word))
       (some seq)))

(defn possible-domain? [s]
  (if (match-word p-tld (.toLowerCase s)) true false))
