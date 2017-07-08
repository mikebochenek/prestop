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

case class RestaurantSeating(tables: Long, id: Long, restaurant_id: Long, reservation_id: Long, 
    status: Int, day: Date, lastupdate: Date, misc: String)

object RestaurantSeating {

  val simple = {
      get[Long]("restaurant_seating.tables") ~
      get[Long]("restaurant_seating.id") ~
      get[Long]("restaurant_seating.restaurant_id") ~
      get[Long]("restaurant_seating.reservation_id") ~
      get[Int]("restaurant_seating.status") ~
      get[Date]("restaurant_seating.lastupdate") ~
      get[Date]("restaurant_seating.day") ~
      get[Option[String]]("restaurant_owner.misc") map {
        case tables ~ id ~ restaurant_id ~ reservation_id ~ status ~ day ~ lastupdate ~ misc => RestaurantSeating(tables, id, 
            restaurant_id, reservation_id, status, day, lastupdate, misc.getOrElse(null))
      }
  }

  val selectSQL = "select tables, id, restaurant_id, reservation_id, status, day, lastupdate, misc from restaurant_seating "
  
  def findAll(): Seq[RestaurantSeating] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL).as(RestaurantSeating.simple *)
    }
  }

  def findByRestaurant(id: Long): Seq[RestaurantSeating] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where status > 0 and restaurant_id = {restaurant_id}").on(
        'restaurant_id -> id).as(RestaurantSeating.simple *)
    }
  }

  implicit val restaurantSeatingReads = Json.reads[RestaurantSeating]
  implicit val restaurantSeatingWrites = Json.writes[RestaurantSeating]

}

