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

  val settingsForm = Form(
    tuple(
      "tables" -> text,
      "phone" -> text))
  
  def save(id: Long) = IsAuthenticated { username =>
    implicit request => {
      //TODO check that user is either super admin OR is the restaurant owner
      val (tables, phone) = settingsForm.bindFromRequest.get
      
      val settings = RestaurantSeating.getOrCreateDefault(id)
      
      Logger.info("tables: " + tables + " phone: " + phone)
      
      Ok(views.html.reservations(Restaurant.findById(username, id)(0), Reservation.findAll))
    }
  }
  
  val testForm = Form(
    tuple(
      "restaurantID" -> text,
      "userID" -> text,
      "time" -> text,
      "guestCount" -> text,
      "comments" -> text))

  def test() = IsAuthenticated { username =>
    implicit request => {
      var validationErrors = ""
      val (restaurantID, userID, time, guestCount, comments) = testForm.bindFromRequest.get
      Logger.info("test reservation: " + testForm.bindFromRequest.get)
      
      makeReservation(restaurantID.toLong, userID.toLong, time, guestCount.toLong, comments)
      
      Ok(views.html.test(Recommend.testForm, null, null, null, User.getFullUser(username).get.id.toString, 
          "47.385740", "8.518084", "false", "10", "0.0", "100.0", "100", "", "-1"))
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
      val userID = (request.body.asJson.get \ "user_id").as[String].toLong
      val restaurantID = (request.body.asJson.get \ "restaurant_id").as[String].toLong
      val time = (request.body.asJson.get \ "reservationtime").as[String]
      val guestCount = (request.body.asJson.get \ "guestcount").as[String].toInt
      val comments = (request.body.asJson.get \ "special_requests").as[String]
      makeReservation(restaurantID, userID, time, guestCount, comments)
      Ok("ok")
    }
  }
  
  def makeReservation(restaurantID: Long, userID: Long, time: String, guestCount: Long, comments: String) = {
    //TODO validate inputs
    if (restaurantID > 0 && time != null && guestCount > 1) {
      val r = Restaurant.findById("", restaurantID)
      if (r.size > 0) {
        r(0).schedule
      }
    }
    
    //TODO check time against restaurant schedule
    
    //TODO check if table with sufficient seating is available at this time
    
    //TODO create reservation
    val timeObj = parseTime(time)
    val id = Reservation.create(userID, restaurantID, timeObj, guestCount.toInt, comments, 0)
    Logger.info("Reservation created - id: " + id.get + " user: " + userID + " restaurant: " + restaurantID)
    
    //TODO update availability for particular restaurant
    
    //TODO feedback: email, sms, e-mail restaurant?.. 
          
    id
  }
  
  def parseTime(t: String) = {
    new Date(t)
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
