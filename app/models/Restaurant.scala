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
    schedulecron: String, restype: Int, lastupdate: Date, status: Int, 
    phoneNumber: String, email: String, var tags: Seq[String])

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
      get[Int]("restaurant.status") map {
        case id ~ name ~ city ~ address ~ longitude ~ latitude ~ schedulecron ~ restype ~ lastupdate ~ status => 
          Restaurant(id, name, city, address, longitude, latitude, schedulecron, restype, lastupdate, status, null, null, Seq.empty[String])
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
      scheduleCron: String, restype: Int, status: Int) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update restaurant set name = {name}, city = {city}, address = {address},
         longitude = {longitude}, latitude = {latitude}, schedulecron = {schedulecron}, 
         restype = {restype}, lastupdate = {lastupdate}, status = {status} where id = {id}
        """).on(
          'id -> id,
          'name -> name,
          'city-> city,
          'address -> address,
          'longitude -> longitude,
          'latitude-> latitude,
          'schedulecron-> scheduleCron,
          'restype -> restype,
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
      SQL("select id, name, city, address, longitude, latitude, schedulecron, restype, lastupdate, status from restaurant where id = {id}").on(
        'id -> id).as(Restaurant.simple *)
    }
  }
  
  def findAll(): Seq[Restaurant] = {
    DB.withConnection { implicit connection =>
      SQL("select id, name, city, address, longitude, latitude, schedulecron, restype, lastupdate, status from restaurant "
          + " order by id asc").on().as(Restaurant.simple *)
    }
  }
  
  implicit val teamReads = Json.reads[Restaurant]
  implicit val teamWrites = Json.writes[Restaurant]

}