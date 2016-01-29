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

object Activities extends Controller with Secured {

  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling Activities get - load data for id:" + id)
      val all = ActivityLog.findAllByUser(id)
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 

  def create() = Action {
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id")
      val activity_type = (request.body.asJson.get \ "activity_type")
      val activity_subtype = (request.body.asJson.get \ "activity_subtype")
      val activity_details = (request.body.asJson.get \ "activity_details")
      val id = ActivityLog.create(user_id.as[String].toLong, activity_type.as[String].toLong, activity_subtype.as[String].toLong, activity_details.as[String])
      Logger.info("ActivityLog created - id: "+ id.get + " type:" + activity_type.as[String] + " subtype: " + activity_subtype.as[String] +  " user: " + user_id.as[String])
      Ok("ok")
    }
  }
}
