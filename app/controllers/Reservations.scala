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
import common.RecommendationUtils
import java.util.Calendar
import java.text.SimpleDateFormat
import java.text.ParseException

object Reservations extends Controller with Secured {

  val settingsFormat = new SimpleDateFormat("dd.MM.yyyy") //09.07.2017
  val mySQLFormat = new SimpleDateFormat("yyyy-MM-dd") //"2017-07-11"
  def load(id: Long) = IsAuthenticated { username =>
    implicit request => {
      //TODO check that user is either super admin OR is the restaurant owner
      val seating = RestaurantSeating.getOrCreateDefault(id)
      val misc = getPreviousMiscSafely(seating)

      Ok(views.html.reservations(Restaurant.findById(username, id)(0), Reservation.findByRestaurant(id, mySQLFormat.format(new Date())), 
          seating, misc, settingsFormat.format(new Date())))
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
      
      val seating = RestaurantSeating.getOrCreateDefault(id)
      val misc = getPreviousMiscSafely(seating)
      
      misc.reservationsPhone = Option(phone)
      seating.tables = tables.toLong
      seating.misc = Json.toJson(misc).toString
      RestaurantSeating.update(seating)
      
      Logger.info("tables: " + tables + " phone: " + phone)
      
      Ok(views.html.reservations(Restaurant.findById(username, id)(0), Reservation.findByRestaurant(id, mySQLFormat.format(new Date())), 
          seating, misc, settingsFormat.format(new Date())))
    }
  }
  
  def getPreviousMiscSafely(seating: RestaurantSeating) = {
    seating.misc match {
      case null => RestaurantSeatingMisc.default()
      case "" => RestaurantSeatingMisc.default()
      case _ => {
        try {
          Json.parse(seating.misc).validate[RestaurantSeatingMisc].get
        } catch {
          case e: Exception => {
            Logger.info("failed to parse seating.misc, will use default instead " + e)
            RestaurantSeatingMisc.default()
          }
        }
      }
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
        val schedule = r(0).schedule
        
        //TODO check time against restaurant schedule
        val ctime = Calendar.getInstance(RecommendationUtils.timezone)
        val open = RecommendationUtils.checkScheduleForCalendar(schedule, ctime)
        
        Logger.debug("open: " + open)
      }
      
    }

    val requestedTime = parseTime(time)

    
    //check if table with sufficient seating is available at this time
    //1. check total available tables
    val seating = RestaurantSeating.getOrCreateDefault(restaurantID)
    //2. check reservations for this given date AND timeframe
    val existingReservations = Reservation.findByRestaurant(restaurantID, mySQLFormat.format(requestedTime))
    
    val seatingAvailable = checkSeating(seating, existingReservations, requestedTime, guestCount)
    
    //create reservation only if seatingAvailable
    if (seatingAvailable) {
      val id = Reservation.create(userID, restaurantID, requestedTime, guestCount.toInt, comments, 0)
      Logger.info("Reservation created - id: " + id.get + " user: " + userID + " restaurant: " + restaurantID)
    
      //update availability for particular restaurant? probably not needed, since we check all reservations for that day
    
      //TODO notification feedback: email, sms, e-mail restaurant?.. 
          
      id
    } else {
      -1
    }
  }
  
  def checkSeating(seating: RestaurantSeating, reservations: Seq[Reservation], requestedTime: Date, guestCount: Long) = { 
    var runningTotal = 0;
    for (r <- reservations) {
      Logger.debug("id: " + r.id + " time: " + r.reservationtime + " guests: " + r.guestcount)
      if (requestedTime.getHours == r.reservationtime.getHours) { //TODO this needs a better check
        runningTotal += r.guestcount
      }
    }
    Logger.debug("runningTotal: " + runningTotal)
    (runningTotal + guestCount < seating.tables)
  }
  
  // "yyyy.MM.dd G 'at' HH:mm:ss z"	2001.07.04 AD at 12:08:56 PDT
  // "yyyy-MM-dd'T'HH:mm:ss.SSSZ"	2001-07-04T12:08:56.235-0700
  val format = Array(new SimpleDateFormat("dd MMMM yyyy - kk:mm"),
      new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z"),
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
  def parseTime(t: String) = {
    var parsedDate = null: Date
    var idx = 0;
    do {
      try {
        idx = idx + 1
        parsedDate = format(idx-1).parse(t)
      } catch {
        case e: ParseException => Logger.debug("text: " + t + "exception caught: " + e);
      }      
    } while (parsedDate == null && idx < (format.size))
      
    parsedDate  
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
