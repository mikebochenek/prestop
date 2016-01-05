
## About
iDone lets you track and showcase your daily accomplishments

* create an account and start writing in less than a minute
* built with security and privacy as a primary goals
* select sharing options from: private, share with manager, or public
* export your data or integrate to external systems via secure REST API or Webservices
* enter iDone items online, via email, or on your smart phone
* available in English, Español, Français, Deutsch, Italiano, Język Polski, Português, русский язык, 日本語, 中文, हिन्दी, العربية, Türkçe
* completely free for personal use (corporate plans from $10 per month)

Stop making depressing todo lists, instead celebrate your daily accomplishments!

## Technology stack

1. Scala Play Framework 2.2.3
2. AngularJS 1.2.19
3. Bootstrap 3.2.0
4. jQuery 1.11.1 (used by 4)
5. MySQL (or any other database for persistence)

## Deployment
Follow these steps in order to deploy the project on your machine:

1. Download scala and play
2. manually run conf/evolutions/default/*.sql in your database
3. check conf/application.conf (particularly db.* settings)
4. start play, compile, and run

## Thanks to...

This project is heavitly based on The Eventual PlayFramework-AngularJS-Bootstrap-MongoDB Seed Project by **[Sari Haj Hussein](http://sarihh.info)**.

The seed had the following features (to recall a few):

1. It offers a complete [single-page application](http://en.wikipedia.org/wiki/Single-page_application) experience.
2. It uses the asynchronous and non-blocking [ReactiveMongo driver](http://reactivemongo.org/).
3. It supports the new HTML5 routing and histoty API (i.e., no hashbangs, no bullshit).
4. It packs JavaScript libraries inside the solution (i.e., no WebJars, no download time, and again no bullshit).
5. It makes minimal usage of [Play Scala templates](http://www.playframework.com/documentation/2.1.5/ScalaTemplates), thereby clearing the space for AngularJS directives in your HTML.
6. It cleanly separates between Play routes that serve HTML and those that serve JSON.
7. It cleanly separates and optimally maps AngularJS routes to Play routes.
8. It conceals Play routes from end-users, thereby ensuring that all pages are properly styled before they are presented.
