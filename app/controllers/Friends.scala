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
import models.json.FriendSuggestion
import models.json.FriendSuggestion
import models.json.FriendSuggestion
import scala.collection.mutable.MutableList

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
      user.profileImageURL = Image.findByUser(userFull.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url //TODO populate user.profileImageURL
      Ok(Json.prettyPrint(Json.toJson(user)))
    }
  }
  
  def suggestFriendsToFollow() = Action {
    implicit request => {
      Logger.info("HTTP post to /api/friends/suggest with: " + request.body.asJson.get)
      val phones = (request.body.asJson.get \ "phones").as[Array[String]]
      val user = (request.body.asJson.get \ "user_id").asOpt[Long]
      
      Logger.info("suggestFriendsToFollow for user defined: " + user.isDefined + " " + user.getOrElse(-1))
      
      val friends = user.isDefined match {
        case false => Seq.empty[Friend]
        case true => Friend.findAllByUser(user.get)
      }
      friends.foreach(f => Logger.info("suggestFriendsToFollow EXISTIN userID: " + f.user_id + " friend:" + f.friend_user_id))
      
      Logger.info("suggestFriendsToFollow " + phones.length)
      val all = MutableList.empty[FriendSuggestion]
      for (phone <- phones) {
        val user = User.getFullUserByPhone(Settings.cleanPhoneString(phone))
        if (user.size > 0 && !friends.exists(f => f.friend_user_id == user(0).id)) {
          val newFriend = new FriendSuggestion(user(0).id, 
              Image.findByUser(user(0).id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
              user(0).username, user(0).fullname, user(0).phone)
          Logger.info("suggesting: " + newFriend)
          all += newFriend
        }
      }
      Ok(Json.prettyPrint(Json.toJson(all)))
    }
  }
  
  def checkAndConvertUserID(id: String) : Long = {
      val user = User.getFullUserByUsername(id)
      val user_id = user.isDefined match {
        case false => id.toLong
        case true => user.get.id
      }
      user_id
  }

  def create() = Action {
    implicit request => {
      Logger.info("HTTP post to /api/friends/new with: " + request.body.asJson.get)
      val user_id_input = (request.body.asJson.get \ "user_id").as[String]
      val friend_array = (request.body.asJson.get \ "friend_user_id").as[Array[String]]
      for (friend_id_str <- friend_array) {
        val f = checkAndConvertUserID(friend_id_str)
        if (f < 10000000) {
          val id = Friend.create(checkAndConvertUserID(user_id_input), f, 0)
          Logger.info("Create Friend entity with id: " + id.get + " user_id: " + user_id_input + " friend: " + friend_id_str)
        } else {
          Logger.info("SKIPPING Create Friend entity with user_id: " + user_id_input + " friend: " + friend_id_str)
        }
      }
      Ok(Json.prettyPrint(Json.toJson(CommonJSONResponse.OK)))
    }
  }

  def update() = Action {
    implicit request => {
      Logger.info("HTTP post to /api/friends/update with: " + request.body.asJson.get)
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
      Logger.info("HTTP post to /api/invite with: " + request.body.asJson.get)
      val txt = (request.body.asJson.get \ "donetext")
      val restId = (request.body.asJson.get \ "restaurantID")
      val id = 13;
      Logger.info("nothing has been created yet - " + txt.as[String] + " with id:" + id + " restaurantID:"+ restId.as[String].toLong)
      Ok("ok")
    }
  }
  
}
