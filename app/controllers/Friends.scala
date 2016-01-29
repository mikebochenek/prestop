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

object Friends extends Controller with Secured {

  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling Friends getByUser - for id:" + id)
      val all = Friend.findAllByUser(id)
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 
  
  def getUserProfile(id: Long) = Action { 
    implicit request => {
      Logger.info("calling Friends getUserProfile - for id:" + id)
      val userFull = User.getFullUser(id)
      val user = new UserProfile(userFull.id, userFull.email, userFull.username, 0, 0, 0, 0, null)
      user.followers = Friend.findAllFriends(user.id).size
      user.following = Friend.findAllByUser(user.id).size
      user.reservations = Reservation.findAllByUser(user.id).size
      user.likes = ActivityLog.findAllByUser(user.id).size //TODO filtering for likes
      //TODO populate user.profileImageURL
      Ok(Json.prettyPrint(Json.toJson(user)))
    }
  }

  def create() = Action {
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id").as[String].toLong
      val friend_user_id = (request.body.asJson.get \ "friend_user_id").as[String].toLong
      val status = (request.body.asJson.get \ "status").as[String].toInt
      val id = Friend.create(user_id, friend_user_id, status)
      Logger.info("Create Friend endity with id: " + id.get + " user_id: " + user_id)
      Ok("ok")
    }
  }

  def update() = Action {
    implicit request => {
      val id = (request.body.asJson.get \ "id").as[String].toLong
      val user_id = (request.body.asJson.get \ "user_id").as[String].toLong
      val friend_user_id = (request.body.asJson.get \ "friend_user_id").as[String].toLong
      val status = (request.body.asJson.get \ "status").as[String].toInt
      Friend.update(id, user_id, friend_user_id, status)
      Logger.info("Update Friend entity with id: " + id + " user_id: " + user_id)
      Ok("ok")
    }
  }
  
  def invite() = IsAuthenticated { username =>
    implicit request => {
      val txt = (request.body.asJson.get \ "donetext")
      val restId = (request.body.asJson.get \ "restaurantID")
      val id = 13;
      Logger.info("nothing has been created yet - " + txt.as[String] + " with id:" + id + " restaurantID:"+ restId.as[String].toLong)
      Ok("ok")
    }
  }
  
}
