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

case class RestaurantOwner(id: Long, restaurant_id: Long, user_id: Long, status: Int, lastupdate: Date, settings: String)

object RestaurantOwner {

  val simple = {
      get[Long]("restaurant_owner.id") ~
      get[Long]("restaurant_owner.restaurant_id") ~
      get[Long]("restaurant_owner.user_id") ~
      get[Int]("restaurant_owner.status") ~
      get[Date]("restaurant_owner.lastupdate") ~
      get[Option[String]]("restaurant_owner.settings") map {
        case id ~ restaurant_id ~ user_id ~ status ~ lastupdate ~ settings => RestaurantOwner(id, restaurant_id, user_id, status, lastupdate, settings.getOrElse(null))
      }
  }

  def findAll(): Seq[RestaurantOwner] = {
    DB.withConnection { implicit connection =>
      SQL("select id, restaurant_id, user_id, status, lastupdate, settings from restaurant_owner ").as(RestaurantOwner.simple *)
    }
  }

  def findByUser(userid: Long): Seq[RestaurantOwner] = {
    DB.withConnection { implicit connection =>
      SQL("select id, restaurant_id, user_id, status, lastupdate, settings from restaurant_owner where status > 0 and user_id = {userid}").on(
        'userid -> userid).as(RestaurantOwner.simple *)
    }
  }

  implicit val restaurantOwnerReads = Json.reads[RestaurantOwner]
  implicit val restaurantOwnerWrites = Json.writes[RestaurantOwner]

}

