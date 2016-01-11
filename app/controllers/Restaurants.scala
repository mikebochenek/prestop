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


  def getById(id: Long) = IsAuthenticated { username =>
    implicit request => {
      val all = Restaurant.findById(username, id)
      Ok(Json.toJson(all.map(a => Json.toJson(a))))
    }
  } 

  def edit(id: Long) = IsAuthenticated { username =>
    implicit request => {
      val all = Restaurant.findById(username, id)
      Logger.info("calling restaurant edit - load data for id:" + id)
      Ok(views.html.restaurant_edit(restaurantForm, all(0)))
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
      "status" -> text))

  def save = IsAuthenticated { username =>
    implicit request => { 
      val (id, name, city, address, longitude, latitude, schedule, restype, status) = restaurantForm.bindFromRequest.get
      Restaurant.update(id.toLong, name, city, address, longitude.toDouble, latitude.toDouble, schedule, restype.toInt, status.toInt)
      Logger.info("calling restaurant update for id:" + id)
      Redirect(routes.Restaurants.edit(id.toLong))
    }
  }

  def getAll() = IsAuthenticated { username =>
    implicit request => {
      val all = Restaurant.findAll()
      Ok(Json.toJson(all.map(a => Json.toJson(a))))
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
      Image.create(new Image(0, file.getAbsolutePath, Image.createUrl(ts + "/" + file.getName), id, 0, 0, null))
      Redirect(routes.Restaurants.edit(id))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
}
