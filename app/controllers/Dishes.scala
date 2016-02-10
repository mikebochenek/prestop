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
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import play.api.libs.Files

object Dishes extends Controller with Secured {

  def getById(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling dish edit - load data for id:" + id)
      val dish = Dish.findById(username, id)
      val tags = Tag.findByRef(id, 11).map(_.name).mkString(", ")
      val greenscoretags = Tag.findByRef(id, 31).map(_.name).mkString(", ")
      val url = Image.findByDish(id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      Ok(views.html.dish_edit(dishForm, dish(0), url, tags, greenscoretags))
    }
  } 

  
  val dishForm = Form(
    tuple(
      "id" -> text,
      "price" -> text,
      "name" -> text,
      "greenscore" -> text,
      "restaurant_id" -> text,
      "status" -> text,
      "tags" -> text,
      "greenscoretags" -> text))
      
  def save = IsAuthenticated { username =>
    implicit request =>  
      val (id, price, name, greenscore, restaurant_id, status, tags, greenscoretags) = dishForm.bindFromRequest.get
      Dish.update(id.toLong, price.toDouble, name, greenscore.toDouble, status.toInt)
      Tag.updateTags(id.toLong, tags, 11)
      Tag.updateTags(id.toLong, greenscoretags, 31)
      Logger.info("calling restaurant update for id:" + id + " price:" + price + " name:" + name + " tags:" + tags + " greenscoretags: " + greenscoretags)
      Redirect(routes.Dishes.getById(id.toLong))
  }

  def getAll(restId: Long) = Action {
    implicit request => {
      val dishes = Dish.findAll(restId)
      for (dish <- dishes) {      
        dish.tags = Tag.findByRef(dish.id, 11).map(_.name)
        dish.url = Image.findByDish(dish.id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      }

      Ok(Json.prettyPrint(Json.toJson(dishes.map(a => Json.toJson(a)))))
    }
  } 
  
  def getTags() = Action {
    implicit request => {
      val all = Tag.findAll()
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  }

  def create() = IsAuthenticated { username =>
    implicit request => {
      val txt = (request.body.asJson.get \ "donetext")
      val restId = (request.body.asJson.get \ "restaurantID")
      val id = Dish.create(restId.as[String].toLong, 0.0, txt.as[String], 0.0, 0);
      Logger.info("dish created with " + txt.as[String] + " with id:" + id + " restaurantID:"+ restId.as[String].toLong)
      Ok("ok")
    }
  }

  def upload(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Image.saveAndResizeImages(picture, id, "dish")
      Redirect(routes.Dishes.getById(id))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
}
