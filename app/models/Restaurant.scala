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

case class RestaurantFriends(id: Long, name: String, var url: String)

object RestaurantFriends {
  implicit val recommendationFriendsReads = Json.reads[RestaurantFriends]
  implicit val recommendationFriendsWrites = Json.writes[RestaurantFriends]
}

case class RestaurantMiscInfo(postalcode: Option[String], var state: Option[String], var website: Option[String], 
    var country: Option[String], parentRestaurantId: Option[Long], place_id: Option[String], lastupdate: Date)

object RestaurantMiscInfo {
  implicit val restaurantMiscInfoReads = Json.reads[RestaurantMiscInfo]
  implicit val restaurantMiscInfoWrites = Json.writes[RestaurantMiscInfo]
}

case class Restaurant(id: Long, var name: String, var city: String, var address: String, var longitude: Double, var latitude: Double, 
    schedule: String, var open_now: Boolean, restype: Int, status: Int, 
    var phone: Option[String], email: Option[String], postalcode: Option[String], var state: Option[String], var website: Option[String],
    var url: String, var smallurl: String, var paymentoptions: Seq[String], var cuisines: Seq[String],
    var friendsWhoBooked: Seq[RestaurantFriends], misc: RestaurantMiscInfo)

object Restaurant {
  val simple = {
    get[Long]("restaurant.id") ~
      get[String]("restaurant.name") ~
      get[String]("restaurant.city") ~
      get[String]("restaurant.address") ~
      get[Double]("restaurant.longitude") ~
      get[Double]("restaurant.latitude") ~
      get[String]("restaurant.schedulecron") ~
      get[Int]("restaurant.restype") ~
      get[Int]("restaurant.status") ~
      get[Option[String]]("restaurant.phone") ~
      get[Option[String]]("restaurant.email") ~
      get[Option[String]]("restaurant.postalcode") ~
      get[Option[String]]("restaurant.state") ~
      get[Option[String]]("restaurant.country") ~
      get[Option[String]]("restaurant.google_places_id") ~
      get[Date]("restaurant.lastupdate") ~
      get[Option[Long]]("restaurant.parent_id") ~
      get[Option[String]]("restaurant.website") map {
        case id ~ name ~ city ~ address ~ longitude ~ latitude ~ schedulecron ~ restype 
        ~ status ~ phone ~ email ~ postalcode ~ state ~ country ~ google_places_id 
        ~ lastupdate ~ parent_id ~ website => Restaurant(id, name, city, address, 
              longitude, latitude, schedulecron, true, 
              restype, status, phone, email, postalcode, state, website, null, null, 
              Seq.empty[String], Seq.empty[String], Seq.empty[RestaurantFriends], 
              RestaurantMiscInfo(postalcode, state, website, country, parent_id, google_places_id, lastupdate))
      }
  }

  def create(name: String, city: String, address: String, longitude: Double, latitude: Double, 
      scheduleCron: String, restype: Int, parent_id: Option[Long], status: Long): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into restaurant (name, parent_id, city, address, longitude, latitude, schedulecron, restype, lastupdate, status) values (
          {name}, {parent_id}, {city}, {address}, {longitude}, {latitude}, {schedulecron}, {restype}, {lastupdate}, {status}
          )
        """).on(
          'name -> name,
          'parent_id -> parent_id,
          'city-> city,
          'address -> address,
          'longitude -> longitude,
          'latitude-> latitude,
          'schedulecron-> scheduleCron,
          'restype -> restype,
          'lastupdate -> new Date(),
          'status -> status).executeInsert()
    }
  }

  def createOwner(restaurant_id: Long, user_id: Long, settings: String): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into restaurant_owner (restaurant_id, user_id, lastupdate, status, settings) values (
          {restaurant_id}, {user_id}, {lastupdate}, {status}, {settings}
          )
        """).on(
          'restaurant_id -> restaurant_id,
          'user_id -> user_id,
          'settings-> settings,
          'lastupdate -> new Date(),
          'status -> 0).executeInsert()
    }
  }

  def update(id: Long, name: String, city: String, address: String, longitude: Double, latitude: Double, 
      scheduleCron: String, restype: Int, status: Int, phone: String, email: String, postalcode: String, state: String, 
      country: String, website: String, google_places_id: String) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update restaurant set name = {name}, city = {city}, address = {address},
         longitude = {longitude}, latitude = {latitude}, schedulecron = {schedulecron}, 
         restype = {restype}, lastupdate = {lastupdate}, status = {status}, 
         phone = {phone}, email = {email}, postalcode = {postalcode}, state = {state}, 
         country = {country}, website = {website}, google_places_id = {google_places_id}
         where id = {id}
        """).on(
          'id -> id,
          'name -> name,
          'city-> city,
          'address -> address,
          'longitude -> longitude,
          'latitude-> latitude,
          'schedulecron-> scheduleCron,
          'restype -> restype,
          'phone -> phone,
          'email -> email,
          'postalcode -> postalcode,
          'state -> state,
          'country -> country,
          'website -> website,
          'google_places_id -> google_places_id,
          'lastupdate -> new Date(),
          'status -> status).executeUpdate
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from restaurant ").as(scalar[Long].single)
    }
  }

  val selectString = """select id, name, city, address, longitude, latitude, schedulecron, restype, status, phone, email, 
   postalcode, state, country, google_places_id, lastupdate, parent_id, website from restaurant """
  
  def findById(username: String, id: Long): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL(selectString + " where id = {id} ").on(
        'id -> id).as(Restaurant.simple *)
    }
  }
  
  def findAll(): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL(selectString + " where status >= 0 and parent_id is null "
          + " order by id asc").on().as(Restaurant.simple *)
    }
  }

  def findAllByUser(userId: Long): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL(selectString + " where status >= 0 and parent_id is null "
          + " and id in (select restaurant_id from restaurant_owner where user_id = {user_id} and status >= 0) order by id asc").on(
              'user_id -> userId).as(Restaurant.simple *)
    }
  }

  def findAllByParent(parentId: Long): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL(selectString + " where status >= 0 "
          + " and parent_id = {parent_id} order by id asc").on(
              'parent_id -> parentId).as(Restaurant.simple *)
    }
  }

  def findAllSublocations(): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL(selectString + " where status >= 0 and parent_id is not null "
          + " order by id asc").on().as(Restaurant.simple *)
    }
  }
  
  implicit val restaurantReads = Json.reads[Restaurant]
  implicit val restaurantWrites = Json.writes[Restaurant]
}