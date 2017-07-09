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

case class Reservation(id: Long, user_id: Long, restaurant_id: Long, reservationtime: Date,
    guestcount: Int, special_requests: String, lastupdate: Date, status: Int)

object Reservation {
  val simple = {
      get[Long]("reservation.id") ~
      get[Long]("reservation.user_id") ~
      get[Long]("reservation.restaurant_id") ~
      get[Date]("reservation.reservationtime") ~
      get[Int]("reservation.guestcount") ~
      get[Option[String]]("reservation.special_requests") ~
      get[Date]("reservation.lastupdate") ~
      get[Int]("reservation.status") map {
        case id ~ user_id ~ restaurant_id ~ reservationtime ~ guestcount ~ special_requests ~ lastupdate ~ status => 
          Reservation(id, user_id, restaurant_id, reservationtime, guestcount, special_requests.getOrElse(null), lastupdate, status)
      }
  }

  def create(user_id: Long, restaurant_id: Long, reservationtime: Date, guestcount: Int, 
      special_requests: String, status: Int): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into reservation (user_id, restaurant_id, reservationtime, guestcount, 
            special_requests, lastupdate, status) values (
          {user_id}, {restaurant_id}, {reservationtime}, {guestcount}, {special_requests}, {lastupdate}, {status}
          )
        """).on(
          'user_id -> user_id,
          'restaurant_id -> restaurant_id,
          'reservationtime -> reservationtime,
          'guestcount -> guestcount,
          'special_requests-> special_requests,
          'lastupdate -> new Date(),
          'status -> status).executeInsert()
    }
  }


  def update(id: Long, reservationtime: Date, guestcount: Int, special_requests: String, status: Int) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update reservation set reservationtime = {reservationtime}, guestcount = {guestcount},  
         special_requests = {special_requests}, lastupdate = {lastupdate}, status = {status} where id = {id}
        """).on(
          'id -> id,
          'reservationtime-> reservationtime,
          'guestcount -> guestcount,
          'special_requests -> special_requests,
          'lastupdate -> new Date(),
          'status -> status).executeUpdate
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from reservation").as(scalar[Long].single)
    }
  }

  val selectSQL = "select id, user_id, restaurant_id, reservationtime, guestcount, special_requests, lastupdate, status from reservation  "
  def findById(id: Long): Seq[Reservation] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where id = {id}").on(
        'id -> id).as(Reservation.simple *)
    }
  }
  
  def findAll(): Seq[Reservation] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " order by id asc").on().as(Reservation.simple *)
    }
  }
  
  def findAllByRestaurant(restaurant_id: Long): Seq[Reservation] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where restaurant_id = {restaurant_id}"
          + " order by id asc").on('restaurant_id -> restaurant_id).as(Reservation.simple *)
    }
  }  

  def findByRestaurant(restaurant_id: Long, reservationTime: String): Seq[Reservation] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where restaurant_id = {restaurant_id} "
          + " and reservationtime between {reservationtime_from} and {reservationtime_to} "
          + " order by id asc").on('restaurant_id -> restaurant_id,
              'reservationtime_from -> (reservationTime + " 00:00:00"),
              'reservationtime_to -> (reservationTime + " 23:59:59")).as(Reservation.simple *)
    }
  }  
  
  def findAllByUser(user_id: Long): Seq[Reservation] = {
    DB.withConnection { implicit connection =>
      SQL("select id, user_id, restaurant_id, reservationtime, guestcount, special_requests, lastupdate, status from reservation where user_id = {user_id}"
          + " order by id asc").on('user_id -> user_id).as(Reservation.simple *)
    }
  }  

  implicit val reservationReads = Json.reads[Reservation]
  implicit val reservationWrites = Json.writes[Reservation]

}