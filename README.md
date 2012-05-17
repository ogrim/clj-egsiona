# clj-egsiona

`clj-egsiona` currently only supports the Linux platform, because of the library `clj-obt`.

## Usage

You will need to configure two things before usage:

In `clj-egsiona.tagger-provider` supply the path to the Oslo-Bergen-Tagger directory like so: `(obt/set-obt-path! "/home/ogrim/bin/The-Oslo-Bergen-Tagger")`

A database is used for persistence of tagged texts and geocoder lookup. In `clj-egsiona.db-provider` use `set-db` to configure database settings. No special stuff is done, so you can use SQLite, PostgreSQL, MySQL or whatever.

(set-db
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname  "//localhost:5432/norge-digitalt"
   :user "postgres"
   :password "})


## License

Copyright (C) 2011-2012 Aleksander Skjæveland Larsen

Distributed under the Eclipse Public License, the same as Clojure.
