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
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  } 

  def edit(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling restaurant edit - load data for id:" + id)
      val all = Restaurant.findById(username, id)
      val tags = Tag.findByRef(id).map(_.name) mkString ", "
      val url = Image.findByRestaurant(id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      Ok(views.html.restaurant_edit(restaurantForm, all(0), url, tags))
    }
  }

  val restaurantForm = Form(
    tuple(
      "id" -> text,
      "name" -> text,
      "city" -> text,
      "address" -> text,
      "longitude" -> text,
      "latitude" -> text,
      "schedule" -> text,
      "restype" -> text,
      "status" -> text,
      "tags" -> text))

  def save = IsAuthenticated { username =>
    implicit request => { 
      val (id, name, city, address, longitude, latitude, schedule, restype, status, tags) = restaurantForm.bindFromRequest.get
      Restaurant.update(id.toLong, name, city, address, longitude.toDouble, latitude.toDouble, schedule, restype.toInt, status.toInt)
      Tag.updateTags(id.toLong, tags)
      Logger.info("calling restaurant update for id:" + id)
      Redirect(routes.Restaurants.edit(id.toLong))
    }
  }

  def getAll() = Action {
    implicit request => {
      val all = Restaurant.findAll()
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
      import java.io.File
      val filename = picture.filename
      val contentType = picture.contentType
      val ts = System.currentTimeMillis()
      val path = "/home/mike/data/presto/" + ts
      val file = new File(path + s"/$filename")
      picture.ref.moveTo(file)
      Image.updateRestaurantImages(id, -1)
      Image.create(new Image(0, file.getAbsolutePath, Image.createUrl(ts + "/" + file.getName), id, 0, 0, null))
      Redirect(routes.Restaurants.edit(id))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
}
