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

  def getLikesByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling getlikes (dish likes - getLikesByUser) id:" + id)
      val all = ActivityLog.findAllByUser(id).filter { x => x.activity_type == 11 } // TODO this should be optimized on the DB
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 
  
  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling get activities (getByUser) id:" + id)
      val all = ActivityLog.findAllByUser(id)
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 

  def getByDish(id: Long) = Action { 
    implicit request => {
      Logger.info("calling getlikers (getByDish) id:" + id)
      val activities = ActivityLog.findAll().filter { x => x.activity_type == 11 && x.activity_subtype == id } // TODO this should be optimized on the DB as well
      val allUsers = activities.map(a => User.getFullUser(a.user_id)) //TODO this should be optimized as well
      val all = allUsers.distinct.map(userFull => new UserProfile(userFull.id, userFull.email, userFull.username, 0, 0, 0, 0, null))
      all.foreach { user => user.profileImageURL = Image.findByUser(user.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url } 
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 

  def like(userId: Long, dishId: Long) = Action { 
    implicit request => {
      Logger.info("calling like for dishid:" + dishId + " userid:" + userId)
      val id = ActivityLog.create(userId, 11, dishId, "")
      Logger.info("ActivityLog created - id: "+ id.get + " type: 11 and subtype: " + dishId +  " user: " + userId)
      Ok("ok")
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
