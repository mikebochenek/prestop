
## About
This is the backend for presto.ch

* basic user management (create/update account)
* manage restaurants and menus
* API for mobile apps

You can quickly see a [running demo here](http://107.170.22.233:8069)
(the demo environment is maintained manually, which means it's not always the latest greatest code)

## Technology stack

1. Scala Play Framework 2.2.3
2. AngularJS 1.2.19
3. Bootstrap 3.2.0
4. jQuery (1.11.1?)
5. MySQL (or any other database for persistence)

## Deployment
Follow these steps in order to deploy the project on your machine:

1. Download scala and play
2. manually run conf/evolutions/default/*.sql in your database
3. check conf/application.conf (particularly db.* settings)
4. start play, compile, and run
