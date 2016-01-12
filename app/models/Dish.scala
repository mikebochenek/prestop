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

case class Dish(id: Long, restaurant_id: Long, price: Double, name: String, vegetarian: Int, 
    gluton: Int, diary: Int, greenScore: Double, lastupdate: Date, status: Int)

object Dish {
  val simple = {
      get[Long]("dish.id") ~
      get[Long]("dish.restaurant_id") ~
      get[Double]("dish.price") ~
      get[String]("dish.name") ~
      get[Int]("dish.vegetarian") ~
      get[Int]("dish.gluton") ~
      get[Int]("dish.diary") ~
      get[Double]("dish.greenscore") ~
      get[Date]("dish.lastupdate") ~
      get[Int]("dish.status") map {
        case id ~ restaurant_id ~ price ~ name ~ vegetarian ~ gluton ~ diary ~ greenscore ~ lastupdate ~ status => 
          Dish(id, restaurant_id, price, name, vegetarian, gluton, diary, greenscore, lastupdate, status)
      }
  }

  def create(restaurant_id: Long, price: Double, name: String, vegetarian: Int, gluton: Int, diary: Int,
      greenscore: Double, status: Int): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into dish (restaurant_id, price, name, vegetarian, gluton, diary, greenscore, lastupdate, status) values (
          {restaurant_id}, {price}, {name}, {vegetarian}, {gluton}, {diary}, {greenscore}, {lastupdate}, {status}
          )
        """).on(
          'restaurant_id -> restaurant_id,
          'price -> price,
          'name -> name,
          'vegetarian-> vegetarian,
          'gluton -> gluton,
          'diary -> diary,
          'greenscore -> greenscore,
          'lastupdate -> new Date(),
          'status -> 0).executeInsert()
    }
  }


  def update(id: Long, price: Double, name: String, vegetarian: Int, gluton: Int, diary: Int,
      greenscore: Double, status: Int) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update dish set price = {price}, name = {name}, vegetarian = {vegetarian}, gluton = {gluton}, 
         diary = {diary}, greenscore = {greenscore}, lastupdate = {lastupdate}, status = {status} where id = {id}
        """).on(
          'id -> id,
          'price -> price,
          'name -> name,
          'vegetarian -> vegetarian,
          'gluton -> gluton,
          'diary -> diary,
          'greenscore -> greenscore,
          'lastupdate -> new Date(),
          'status -> status).executeUpdate
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from dish ").as(scalar[Long].single)
    }
  }

  def findById(username: String, id: Long): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select id, restaurant_id, price, name, vegetarian, gluton, diary, greenscore, lastupdate, status from dish where id = {id}").on(
        'id -> id).as(Dish.simple *)
    }
  }
  
  def findAll(): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select id, restaurant_id, price, name, vegetarian, gluton, diary, greenscore, lastupdate, status from dish "
          + " order by id asc").on().as(Dish.simple *)
    }
  }
  
  def findAll(restaurant: Long): Seq[Dish] = {
    DB.withConnection { implicit connection =>
      SQL("select id, restaurant_id, price, name, vegetarian, gluton, diary, greenscore, lastupdate, status from dish where restaurant_id = {restaurant_id}"
          + " order by id asc").on('restaurant_id -> restaurant).as(Dish.simple *)
    }
  }  
  implicit val teamReads = Json.reads[Dish]
  implicit val teamWrites = Json.writes[Dish]

}