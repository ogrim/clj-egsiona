# clj-egsiona

A library for finding locations in Norwegian texts. Through development, focus have been on processing online news articles.

`clj-egsiona` currently only supports the Linux platform, because of the The-Oslo-Bergen-Tagger's supported platforms. You can get around this by using [clj-obt-service](https://github.com/ogrim/clj-obt-service), which exposes the functionality of [clj-obt](https://github.com/ogrim/clj-obt) as a web service. This makes it possible to use `clj-egsiona` in a Windows setting, either from an external server or locally with a virtual machine running Linux.

Through development, this project was evaluated whith a corpus of 113 articles restricted mainly to the region of Hordaland. The metrics achieved was recall of 93.6%, precision of 69.1% and f-measure of 76.8%. It has not been tested thoroughly on other data sets, so your mileage may vary.


## Setup

`clj-egsiona` is on [Clojars](https://clojars.org/clj-egsiona).

    [clj-egsiona "0.1.0-SNAPSHOT"]

At the very least you need to configure OBT, but it's recommended to use a database for caching.

In `clj-egsiona.core` you need to configure the Oslo-Bergen-Tagger:

    (set-obt "/home/ogrim/bin/The-Oslo-Bergen-Tagger")

to use a local OBT installation. To use the web service as hosted by `clj-obt-service`:

    (set-obt "10.0.0.2:8085")

A database is used for persistence of tagged texts and geocoder lookup. In `clj-egsiona.core` use `set-db` to configure database settings. No special stuff is done, so you can use SQLite, PostgreSQL, MySQL, etc. PostgreSQL configuration will look like this:

    (set-db {:classname "org.postgresql.Driver"
             :subprotocol "postgresql"
             :subname  "//localhost:5432/database-name"
             :user "postgres"
             :password "password"})

Or if you are using SQLite, it will look like this:

    (set-db {:classname   "org.sqlite.JDBC"
             :subprotocol "sqlite"
             :subname     "database.db"})

If it is the first time using the database, there is a function to create the required tables:

    (create-tables)

## Usage

Call `process-text` to get the result as simple text:

    (process-text "Vennligst finn ut om Stavanger eller Sandnes er lokasjoner. De er begge byer i Rogaland.")

    => ("sandnes" "rogaland" "stavanger")

If you want better granularity, use `process-locations` to get more data:

    (process-locations "Vennligst finn ut om Stavanger eller Sandnes er lokasjoner. De er begge byer i Rogaland.")

    => {:address ("Sandnes"), :counties ("rogaland"), :countries (), :regions (), :eu-route (), :grammar ({:tags ["subst" "prop" "<*>"], :lemma "Stavanger", :word "Stavanger", :i 5} {:tags ["subst" "prop" "<*land>" "<*>"], :lemma "Rogaland", :word "Rogaland", :i 16})}

## License

Copyright (C) 2011-2012 Aleksander Skjæveland Larsen

Distributed under the Eclipse Public License, the same as Clojure.
