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


case class Restaurant(id: Long, name: String, city: String, address: String, longitude: Double, latitude: Double, 
    schedule: String, restype: Int, lastupdate: Date, status: Int, 
    phone: Option[String], email: Option[String], postalcode: Option[String], state: Option[String], 
    var url: String, var paymentoptions: Seq[String], var cuisines: Seq[String])

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
      get[Date]("restaurant.lastupdate") ~
      get[Int]("restaurant.status") ~
      get[Option[String]]("restaurant.phone") ~
      get[Option[String]]("restaurant.email") ~
      get[Option[String]]("restaurant.postalcode") ~
      get[Option[String]]("restaurant.state") map {
        case id ~ name ~ city ~ address ~ longitude ~ latitude ~ schedulecron ~ restype ~ lastupdate ~ status ~ phone ~ email ~ postalcode ~ state => 
          Restaurant(id, name, city, address, longitude, latitude, schedulecron, restype, lastupdate, status, phone, email, postalcode, state, null, Seq.empty[String], Seq.empty[String])
      }
  }

  def create(name: String, city: String, address: String, longitude: Double, latitude: Double, 
      scheduleCron: String, restype: Int): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into restaurant (name, city, address, longitude, latitude, schedulecron, restype, lastupdate, status) values (
          {name}, {city}, {address}, {longitude}, {latitude}, {schedulecron}, {restype}, {lastupdate}, {status}
          )
        """).on(
          'name -> name,
          'city-> city,
          'address -> address,
          'longitude -> longitude,
          'latitude-> latitude,
          'schedulecron-> scheduleCron,
          'restype -> restype,
          'lastupdate -> new Date(),
          'status -> 0).executeInsert()
    }
  }


  def update(id: Long, name: String, city: String, address: String, longitude: Double, latitude: Double, 
      scheduleCron: String, restype: Int, status: Int, phone: String, email: String, postalcode: String, state: String) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update restaurant set name = {name}, city = {city}, address = {address},
         longitude = {longitude}, latitude = {latitude}, schedulecron = {schedulecron}, 
         restype = {restype}, lastupdate = {lastupdate}, status = {status},
         phone = {phone}, email = {email}, postalcode = {postalcode}, state = {state} 
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
          'lastupdate -> new Date(),
          'status -> status).executeUpdate
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from restaurant ").as(scalar[Long].single)
    }
  }

  def findById(username: String, id: Long): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL("select id, name, city, address, longitude, latitude, schedulecron, restype, lastupdate, status, phone, email, postalcode, state from restaurant where id = {id}").on(
        'id -> id).as(Restaurant.simple *)
    }
  }
  
  def findAll(): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL("select id, name, city, address, longitude, latitude, schedulecron, restype, lastupdate, status, phone, email, postalcode, state from restaurant where status >= 0 "
          + " order by id asc").on().as(Restaurant.simple *)
    }
  }
  
  implicit val restaurantReads = Json.reads[Restaurant]
  implicit val restaurantWrites = Json.writes[Restaurant]

}