(ns clj-egsiona.core
  (:require [clj-obt.tools :as obt]
            [clj-egsiona.tagger-provider :as tag]
            [clj-egsiona.db-provider :as db]
            [clojure.string :as str]
            [clj-egsiona.processing :as p])
  (:use [ogrim.common.tools]))

(defn set-db [db-spec]
  (do (db/set-db db-spec)
      (tag/reset-db)
      true))

(defn set-obt [location]
  (let [service? (-> (str/split location #":") last num?)]
    (if service? (tag/set-obt-service location) (tag/set-obt-program location))))

(defn create-tables []
  (db/create-tables))

(defn process-text [s]
  (p/process-text s))

(defn process-locations [s]
  (p/process-locations s))
