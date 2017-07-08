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

case class RestaurantSeating(tables: Long, var id: Long, restaurant_id: Long, reservation_id: Long, 
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

  def create(seating: RestaurantSeating): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into restaurant_seating (tables, restaurant_id, reservation_id, status, day, lastupdate, misc) values (
          {tables}, {restaurant_id}, {reservation_id}, {status}, {day}, {lastupdate}, {misc}
          )
        """).on(
          'tables -> seating.tables,
          'restaurant_id -> seating.restaurant_id,
          'reservation_id -> seating.reservation_id,
          'status -> seating.status,
          'day-> seating.day,
          'lastupdate -> new Date(),
          'misc -> seating.misc).executeInsert()
    }
  }


  def update(seating: RestaurantSeating) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update restaurant_seating set tables = {tables}, restaurant_id = {restaurant_id},  
         reservation_id = {reservation_id}, lastupdate = {lastupdate}, status = {status},
         day = {day}, misc = {misc} where id = {id}
        """).on(
          'id -> seating.id,
          'tables -> seating.tables,
          'restaurant_id -> seating.restaurant_id,
          'reservation_id -> seating.reservation_id,
          'status -> seating.status,
          'day-> seating.day,
          'lastupdate -> new Date(),
          'misc -> seating.misc).executeUpdate
    }
  }
  
  val selectSQL = "select tables, id, restaurant_id, reservation_id, status, day, lastupdate, misc from restaurant_seating "
  
  def findAll(): Seq[RestaurantSeating] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL).as(RestaurantSeating.simple *)
    }
  }
  
  def getOrCreateDefault(id: Long) : RestaurantSeating = {
    val existing = getSettingsByRestaurant(id)
    existing.getOrElse( { createDefaultInsert(id) })
  }
  
  def createDefaultInsert(id: Long) : RestaurantSeating = { 
    val r = new RestaurantSeating(25, -1, id, 0, 0, new Date(), null, "")
    r.id = create(r).get
    r
  }

  def getSettingsByRestaurant(id: Long): Option[RestaurantSeating] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where reservation_id = 0 and status > 0 and restaurant_id = {restaurant_id}").on(
        'restaurant_id -> id).as(RestaurantSeating.simple.singleOpt)
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

