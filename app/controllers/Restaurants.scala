package controllers

import models.Restaurant
import models.Image
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Session
import views.html
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import java.util.Date
import play.api.Logger
import models.Tag
import models.RestaurantFriends

object Restaurants extends Controller with Secured {


  def index = Action { implicit request =>
    Ok(html.restaurants());
  }

  def about = Action { implicit request =>
    Ok(html.about());
  }

  def contact = Action { implicit request =>
    Ok(html.contact());
  }


  def getById(id: Long) = Action {
    implicit request => {
      val all = Restaurant.findById(null, id)
      for (restaurant <- all) {
        restaurant.paymentoptions =  Tag.findByRef(restaurant.id, 12).map(_.en_text.get) //TODO good, but this has to be dynamic somehow
        restaurant.cuisines =  Tag.findByRef(restaurant.id, 21).map(_.en_text.get) //TODO should be dynamic
        restaurant.url = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
        restaurant.smallurl = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
        
        restaurant.schedule_text = restaurant.schedule.split("\r\n")
        restaurant.open_now = isOpen(restaurant.schedule)
        
        //TODO!
        val friends = List(new RestaurantFriends(1, "Mike Bochenek", Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url))
        restaurant.friendsWhoBooked = friends;
      }
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  }
  
  def isOpen(schedule: String) = {
    val now = new Date();
    val schedule_text = schedule.split("\r\n")
    val day = now.getDay
    // fudge, this is harder than I thought
    true
  }

  def edit(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling restaurant edit - load data for id:" + id)
      val all = Restaurant.findById(username, id)
      val tags = Tag.findByRef(id, 12).map(_.name).mkString(", ")
      val cuisines = Tag.findByRef(id, 21).map(_.name).mkString(", ")
      val url = Image.findByRestaurant(id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      Ok(views.html.restaurant_edit(restaurantForm, all(0), url, tags, cuisines))
    }
  }

  val restaurantForm = Form(
    tuple(
      "id" -> text,
      "name" -> text,
      "phone" -> text,
      "email" -> text,
      "address" -> text,
      "city" -> text,
      "postalcode" -> text,
      "state" -> text,
      "longitude" -> text,
      "latitude" -> text,
      "schedule" -> text,
      "restype" -> text,
      "status" -> text,
      "cuisines" -> text,
      "tags" -> text))

  def save = IsAuthenticated { username =>
    implicit request => { 
      val (id, name, phone, email, address, city, postalcode, state, longitude, latitude, schedule, restype, status, cuisines, tags) = restaurantForm.bindFromRequest.get
      Restaurant.update(id.toLong, name, city, address, longitude.toDouble, latitude.toDouble, schedule, restype.toInt, status.toInt, phone, email, postalcode, state)
      Tag.updateTags(id.toLong, tags, 12)
      Tag.updateTags(id.toLong, cuisines, 21)
      Logger.info("calling restaurant update for id:" + id)
      Redirect(routes.Restaurants.edit(id.toLong))
    }
  }

  def getAll() = Action {
    implicit request => {
      val all = Restaurant.findAll()
      for (restaurant <- all) {
        restaurant.paymentoptions =  Tag.findByRef(restaurant.id, 12).map(_.name)
        restaurant.cuisines =  Tag.findByRef(restaurant.id, 21).map(_.name)
        restaurant.url = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      }
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  } 

  def create() = IsAuthenticated { username =>
    implicit request => {
      val txt = (request.body.asJson.get \ "donetext")
      val id = Restaurant.create(txt.as[String], "", "", 0.0, 0.0, "", 0);
      Logger.info("restaurant created with " + txt.as[String] + " with id:" + id)
      Ok("ok")
    }
  }
  
  def upload(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Image.saveAndResizeImages(picture, id, "restaurant")
      Redirect(routes.Restaurants.edit(id))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
}
