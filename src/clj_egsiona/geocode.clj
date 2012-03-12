(ns clj-egsiona.geocode
  (:require [geocoder.core :as g]
            [clj-time.core :as t]
            [ogrim.common.db :as db]
            [clojure.java.jdbc :as sql])
  (:use [clj-egsiona.db-provider]))

(def ^{:dynamic true :private true} *reset-time* (atom nil))
(def ^{:dynamic true :private true} *api-calls* (atom 0))
(def ^{:private true} api-max-requests 2500)
(def ^{:dynamic true :private true}  *db* (atom (get-db)))

(defn reset-db []
  (reset! *db* (get-db)))

(defn- reset-time []
  (reset! *reset-time* (t/plus (t/now) (t/hours 24))))

(defn- can-call-api? []
  (cond (not @*reset-time*) (do (reset-time) true)
        (< @*api-calls* api-max-requests) true
        (t/after? (t/now) @*reset-time*) (do (reset-time) (reset! *api-calls* 0) true)
        :else false))

(defn- db-locations [location]
  (if (nil? @*db*) nil
      (let [[loc] (db/query-db @*db* ["SELECT * FROM location WHERE id = ?" (.toUpperCase location)])]
        (:location loc))))

(defn- persist-geocoded [location geocoded]
  (cond (nil? @*db*) false
        (empty? (db-locations location))
        (do (db/insert-row @*db* "location" [(.toUpperCase location) (str "(" (apply str geocoded) ")")])
            true)))

(defn geocode [address]
  (let [location (db-locations address)]
    (if (seq location)
      (read-string location)
      (if can-call-api?
        (let [geocoded (g/geocode-address address)]
          (do (persist-geocoded address geocoded)
                  geocoded))))))
