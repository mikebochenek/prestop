package controllers

import java.io.File
import play.Play
import play.api.mvc.Action
import play.api.mvc.Session
import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import models._
import views._
import java.util.Date
import models.json.DishLikers

object Reservations extends Controller with Secured {

  def load(id: Long) = IsAuthenticated { username =>
    implicit request => {
      //TODO check that user is either super admin OR is the restaurant owner
      Ok(views.html.reservations(Restaurant.findById(username, id)(0), Reservation.findAll))
    }
  }

  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling Activities get - load data for id:" + id)
      val all = Reservation.findAllByUser(id)
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 
  
  def getByRestaurant(id: Long)  = Action { 
    implicit request => {
      Logger.info("calling ByReservations - load data for id:" + id)
      val reservations = Reservation.findAllByRestaurant(id)
      val allUsers = reservations.map(a => User.getFullUser(a.user_id)) //TODO this should be optimized as well
      val all = allUsers.distinct.map(user => new DishLikers(user.id, 
         Image.findByUser(user.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
         user.fullname, user.city, id))
      
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 
  
  def create() = Action {
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id").as[String].toLong
      val restaurant_id = (request.body.asJson.get \ "restaurant_id").as[String].toLong
      val reservationtime = (request.body.asJson.get \ "reservationtime").as[String].toLong
      val guestcount = (request.body.asJson.get \ "guestcount").as[String].toInt
      val special_requests = (request.body.asJson.get \ "special_requests").as[String]
      val id = Reservation.create(user_id, restaurant_id, 
          new Date(reservationtime), guestcount, special_requests, 0)
      Logger.info("Reservation created - id: " + id.get + " user: " + user_id + " restaurant: " + restaurant_id)
      Ok("ok")
    }
  }

  def update() = Action {
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id").as[String].toLong
      // changing restaurant does not make sense in this case.. restaurant_id")
      val reservationtime = (request.body.asJson.get \ "reservationtime").as[String].toLong
      val guestcount = (request.body.asJson.get \ "guestcount").as[String].toInt
      val special_requests = (request.body.asJson.get \ "special_requests").as[String]
      val id = (request.body.asJson.get \ "id").as[String].toLong
      val status = (request.body.asJson.get \ "status").as[String].toInt
      Reservation.update(id, new Date(reservationtime), guestcount, special_requests, status)
      Logger.info("Reservation updated id:" + id + " user_id:"+ user_id)
      Ok("ok")
    }
  }
}
