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

object Reservations extends Controller with Secured {

  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling Activities get - load data for id:" + id)
      val all = Reservation.findAllByUser(id)
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 

  def create() = Action {
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id")
      val restaurant_id = (request.body.asJson.get \ "restaurant_id")
      val reservationtime = (request.body.asJson.get \ "reservationtime")
      val guestcount = (request.body.asJson.get \ "guestcount")
      val special_requests = (request.body.asJson.get \ "special_requests")
      val id = Reservation.create(user_id.as[String].toLong, restaurant_id.as[String].toLong, new Date(reservationtime.as[String].toLong), guestcount.as[String].toInt, special_requests.as[String], 0)
      Logger.info("Reservation created - id: " + id.get + " user: " + user_id.as[String]+ " restaurant: " + restaurant_id.as[String])
      Ok("ok")
    }
  }

  def update() = Action {
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id")
      val restaurant_id = (request.body.asJson.get \ "restaurant_id")
      val reservationtime = (request.body.asJson.get \ "reservationtime")
      val guestcount = (request.body.asJson.get \ "guestcount")
      val special_requests = (request.body.asJson.get \ "special_requests")
      // TODO id and status and update   call
      val txt = (request.body.asJson.get \ "donetext")
      val restId = (request.body.asJson.get \ "restaurantID")
      //val id = Dish.create(restId.as[String].toLong, 0.0, txt.as[String], 0, 0, 0, 0.0, 0);
      val id = 13;
      Logger.info("nothing has been created yet - " + txt.as[String] + " with id:" + id + " restaurantID:"+ restId.as[String].toLong)
      Ok("ok")
    }
  }
  
}
