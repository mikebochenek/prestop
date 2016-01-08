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
  
  def getById(id: Long) = IsAuthenticated { username =>
    implicit request => {
      val dish = Dish.findById(username, id)
      Ok(Json.toJson(dish.map(a => Json.toJson(a))))
    }
  } 
  
  def save = IsAuthenticated { username =>
    implicit request => { 
      //TODO
      Redirect(routes.Dishes.getById(1)) //TODO not always 1... :-)
    }
  }

  def getAll(restId: Long) = IsAuthenticated { username =>
    implicit request => {
      val dishes = Dish.findAll(restId)
      Ok(Json.toJson(dishes.map(a => Json.toJson(a))))
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

}
