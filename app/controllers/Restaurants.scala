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
import models.Reservation
import models.User
import models.RestaurantOwner
import models.UserFull

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
        val defaultImage = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 750 && x.status == 0}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        restaurant.url = defaultImage.url
        restaurant.smallurl = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 72 && x.status == 1}.headOption.getOrElse(defaultImage).asInstanceOf[Image].url
        
        restaurant.schedule_text = restaurant.schedule.split("\r\n")
        restaurant.open_now = common.RecommendationUtils.checkSchedule(restaurant.schedule)
        
        if (restaurant.city != null) {
          restaurant.city = restaurant.city + ", Switzerland" //TODO remove this when we add country field
        }
        
        val friends = List(new RestaurantFriends(1, "Mike Bochenek", Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url))
        restaurant.friendsWhoBooked = friends;
      }
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  }

  def edit(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling restaurant edit - load data for id:" + id)
      val all = Restaurant.findById(username, id)
      val tags = Tag.findByRef(id, 12).map(_.name).mkString(", ")
      val cuisines = Tag.findByRef(id, 21).map(_.name).mkString(", ")
      val url = Image.findByRestaurant(id).filter { x => x.status == 0 }.sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      val logourl = Image.findByRestaurant(id).filter { x => x.status == 1 }.sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      val reservations = Reservation.findAllByRestaurant(id)
      val childRestaurants = Restaurant.findAllByParent(id)
      Ok(views.html.restaurant_edit(restaurantForm, all(0), url, logourl, tags, cuisines, reservations, childRestaurants))
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
      "website" -> text,
      "longitude" -> text,
      "latitude" -> text,
      "schedule" -> text,
      "restype" -> text,
      "status" -> text,
      "cuisines" -> text,
      "tags" -> text))

  def save = IsAuthenticated { username =>
    implicit request => { 
      val (id, name, phone, email, address, city, postalcode, state, website, longitude, latitude, schedule, restype, status, cuisines, tags) = restaurantForm.bindFromRequest.get
      Restaurant.update(id.toLong, name, city, address, longitude.toDouble, latitude.toDouble, schedule, restype.toInt, status.toInt, phone, email, postalcode, state, website)
      Tag.updateTags(id.toLong, tags, 12)
      Tag.updateTags(id.toLong, cuisines, 21)
      Logger.info("calling restaurant update for id:" + id)
      Redirect(routes.Restaurants.edit(id.toLong))
    }
  }
  
  def getRestaurantsForUser(user: UserFull): Seq[Restaurant] = user.ttype match {
    case "7" => Restaurant.findAll()
    case _   => Restaurant.findAllByUser(user.id)
  }

  def getAll() = IsAuthenticated { username =>
    implicit request => {
      val user = User.getFullUser(username)
      val all = getRestaurantsForUser(user)
      /* if (!user.ttype.equals("7")) {
        val ro = RestaurantOwner.findByUser(user.id)
        Logger.info("find restaurants that user owns: " + user.id + "  found:" + ro.size)
      } */
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
      val id = Restaurant.create(txt.as[String], "", "", 0.0, 0.0, "", 0, None);
      Logger.info("restaurant created with " + txt.as[String] + " with id:" + id)
      Ok("ok")
    }
  }
  
  def upload(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Image.saveAndResizeImages(picture, id, "restaurant")
      Redirect(routes.Dishes.cropImage(id, "restaurant"))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
  
  def uploadLogo(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("logopicture").map { picture =>
      Image.saveAndResizeImages(picture, id, "restaurantlogo")
      Redirect(routes.Dishes.cropImage(id, "restaurantProfile"))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  

  val restaurantLocationsForm = Form(
    tuple(
      "id" -> text,
      "phone" -> text,
      "email" -> text,
      "address" -> text,
      "city" -> text,
      "postalcode" -> text,
      "state" -> text,
      "longitude" -> text,
      "latitude" -> text,
      "schedule" -> text,
      "status" -> text))
  
  def saveLocations = IsAuthenticated { username =>
    implicit request => { 
      val (id, phone, email, address, city, postalcode, state, longitude, latitude, schedule, status) = restaurantLocationsForm.bindFromRequest.get
      Logger.info("calling restaurant locations edit - load data for id:" + id)
      val rest = Restaurant.findById(username, id.toLong)(0)
      Restaurant.update(id.toLong, rest.name, city, address, longitude.toDouble, latitude.toDouble, schedule, rest.restype, status.toInt, phone, email, postalcode, state, rest.website.getOrElse(""))
      Logger.info("done calling restaurant CHILD update for id:" + id)
      Ok(views.html.restaurant_locations(restaurantLocationsForm, Restaurant.findById(username, id.toLong)(0)))
    }
  }
  
  def editLocations(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling restaurant locations load - for id:" + id)
      val all = Restaurant.findById(username, id.toLong)
      Ok(views.html.restaurant_locations(restaurantLocationsForm, all(0)))
    }
  }

  def addLocations(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling add new location - for id:" + id)
      val r = Restaurant.findById(username, id)(0)
      val newid = Restaurant.create(r.name, r.city, r.address, r.longitude, r.latitude, r.schedule, r.restype, Option(r.id));
      Redirect(routes.Restaurants.edit(id))
    }
  }
}
