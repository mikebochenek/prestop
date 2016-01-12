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

object Dishes extends Controller with Secured {
  
  val blankImage = new Image(0, null, null, 0, 0, 0, null)
  def getById(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling dish edit - load data for id:" + id)
      val dish = Dish.findById(username, id)
      val url = Image.findByDish(id).headOption.getOrElse(blankImage).asInstanceOf[Image].url
      Ok(views.html.dish_edit(dishForm, dish(0), url))
    }
  } 

  
  val dishForm = Form(
    tuple(
      "id" -> text,
      "price" -> text,
      "name" -> text,
      "vegetarian" -> text,
      "gluton" -> text,
      "diary" -> text,
      "greenscore" -> text,
      "restaurant_id" -> text,
      "status" -> text))
      
  def save = IsAuthenticated { username =>
    implicit request => { 
      val (id, price, name, vegetarian, gluton, diary, greenscore, restaurant_id, status) = dishForm.bindFromRequest.get
      Dish.update(id.toLong, price.toDouble, name, vegetarian.toInt, gluton.toInt, diary.toInt, greenscore.toDouble, status.toInt)
      Logger.info("calling restaurant update for id:" + id + " price:" + price + " name:" + name + " greenscore:" + greenscore)
      Redirect(routes.Dishes.getById(id.toLong))
    }
  }

  def getAll(restId: Long) = IsAuthenticated { username =>
    implicit request => {
      val dishes = Dish.findAll(restId)
      Ok(Json.prettyPrint(Json.toJson(dishes.map(a => Json.toJson(a)))))
    }
  } 

  def create() = IsAuthenticated { username =>
    implicit request => {
      val txt = (request.body.asJson.get \ "donetext")
      val restId = (request.body.asJson.get \ "restaurantID")
      val id = Dish.create(restId.as[String].toLong, 0.0, txt.as[String], 0, 0, 0, 0.0, 0);
      Logger.info("dish created with " + txt.as[String] + " with id:" + id + " restaurantID:"+ restId.as[String].toLong)
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
      Image.create(new Image(0, file.getAbsolutePath, Image.createUrl(ts + "/" + file.getName), 0, id, 0, null))
      Redirect(routes.Dishes.getById(id))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
}
