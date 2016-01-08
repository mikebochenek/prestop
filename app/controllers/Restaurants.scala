package controllers

import models.Restaurant
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Session
import views.html
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
}
