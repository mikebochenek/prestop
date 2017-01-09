package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import scala.language.postfixOps
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

case class Dish(id: Long, restaurant_id: Long, price: Double, var name: String, priceBucket: Option[String], serving: Option[String], description: Option[String], 
    var greenScore: Double, lastupdate: Date, status: Int, source: String, var url: String, var distance: Double, var tags: Seq[String])

object Dish {
  val simple = {
      get[Long]("dish.id") ~
      get[Long]("dish.restaurant_id") ~
      get[Double]("dish.price") ~
      get[String]("dish.name") ~
      get[Option[String]]("dish.price_bucket") ~
      get[Option[String]]("dish.serving") ~
      get[Option[String]]("dish.description") ~
      get[Double]("dish.greenscore") ~
      get[Date]("dish.lastupdate") ~
      get[Int]("dish.status") ~
      get[Option[String]]("dish.source") map {
        case id ~ restaurant_id ~ price ~ name ~ price_bucket ~ serving ~ description ~ greenscore ~ lastupdate ~ status ~ source => 
          Dish(id, restaurant_id, price, name, price_bucket, serving, description, greenscore, lastupdate, status, source.getOrElse(""), null, 0.0, Seq.empty[String])
      }
  }

  def create(restaurant_id: Long, price: Double, name: String, greenscore: Double, status: Int): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into dish (restaurant_id, price, name, greenscore, lastupdate, status) values (
          {restaurant_id}, {price}, {name}, {greenscore}, {lastupdate}, {status}
          )
        """).on(
          'restaurant_id -> restaurant_id,
          'price -> price,
          'name -> name,
          'greenscore -> greenscore,
          'lastupdate -> new Date(),
          'status -> status).executeInsert()
    }
  }


  def update(id: Long, price: Double, name: String, greenscore: Double, status: Int, serving: String, 
      description: String, source: String, restaurant_id: Long) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update dish set price = {price}, name = {name}, serving = {serving}, description = {description}, source = {source},
         greenscore = {greenscore}, lastupdate = {lastupdate}, restaurant_id = {restaurant_id}, status = {status} where id = {id}
        """).on(
          'id -> id,
          'price -> price,
          'name -> name,
          'serving -> serving,
          'description -> description,
          'greenscore -> greenscore,
          'source -> source,
          'restaurant_id -> restaurant_id,
          'lastupdate -> new Date(),
          'status -> status).executeUpdate
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from dish ").as(scalar[Long].single)
    }
  }
  
  val columns = " id, restaurant_id, price, name, price_bucket, serving, description, greenscore, lastupdate, status, source "

  def findById(username: String, id: Long): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from dish where id = {id}").on(
        'id -> id).as(Dish.simple *)
    }
  }
  
  def findAll(): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from dish  where status >= 0 "
          + " order by id asc").on().as(Dish.simple *)
    }
  }

  def findAllDeleted(): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from dish  where status < 0 order by id asc").on().as(Dish.simple *)
    }
  }
  
  def findAll(restaurant: Long): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from dish where status >= 0 and restaurant_id = {restaurant_id}"
          + " order by id asc").on('restaurant_id -> restaurant).as(Dish.simple *)
    }
  }
  
  def findAllWithoutImages(): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from dish where id not in (select dish_id from image) "
          + " order by id asc").on().as(Dish.simple *)
    }
  }

  /* dishes which will not be recommended because the restaurant is disabled (statue=-1) */
  def findAllInactive(): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from dish where restaurant_id in (select r.id from restaurant r where status < 0) "
          + " order by id asc").on().as(Dish.simple *)
    }
  }
  
  /* Bites/NewPostViewController.swift:	let priceBucketsDict = ["0 – 19.90": "bucketA", "20 – 34.90": "bucketB", "35 – 44.90": "bucketC", "45+": "bucketD"] */
  def getPriceBucketRange(bucket: String) = {
    bucket match {
      case "bucketA" => "0 - 19.90"
      case "bucketB" => "20 - 34.90"
      case "bucketC" => "35 - 44.90"
      case "bucketD" => "45+"
      case _ => "Unexpected bucket: " + bucket
    }
  }
  
  implicit val teamReads = Json.reads[Dish]
  implicit val teamWrites = Json.writes[Dish]

}