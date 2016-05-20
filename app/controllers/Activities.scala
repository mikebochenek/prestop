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
import models.json.DishLikers
import models.json.DishLikes
import scala.collection.mutable.MutableList
import common.RecommendationUtils
import models.json.DishLikesContainer

object Activities extends Controller with Secured {

  def getLikesByUser(id: Long) = Action { 
    implicit request => {
	    Logger.info("calling get activities (getByUser) id:" + id)
      
      //TODO all the below is way too similar to Recommendation.recommend and should be refactored into a common method
      val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)

      var result = DishLikesContainer(MutableList.empty);

      val activities = ActivityLog.findAllByUserType(id, 11).filter { x => x.activity_type == 11} //still needs to be optimized
      Logger.info("activities " + activities + " size:" + activities.size)

      for (ac <- activities) {
        val dish = Dish.findById(null, ac.activity_subtype)(0)
        val allLikes = Activities.getLikeActivitiesByDish(dish.id)
        val like = true
      
        val friendLikedDishURLs = allLikes.map(x => x.profileImageURL)
        //Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url  :: Nil

        val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse(""))
        
        val r = restaurants.get(dish.restaurant_id).head
        val ri = new DishLikes(id, dish.id, dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, like, Dishes.calculateGreenScore(greenscoretags.size), 
          greenscoretags,
          Image.findByDish(dish.id).filter{x => x.width.get == 172}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          Image.findByDish(dish.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          null,
          Tag.findByRef(dish.id, 11).map(_.name),
          r.id,
          r.name, Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
          friendLikedDishURLs,
          Tag.findByRef(dish.id, 34).map(_.name),
          Tag.findByRef(dish.id, 35).map(_.name),
          Tag.findByRef(dish.id, 36).map(_.name))
        result.likes += ri
      }
      
      Ok(Json.prettyPrint(Json.toJson(result.likes.map(a => Json.toJson(a)))))
	  }
  } 
  
  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling get activities (getByUser) id:" + id)
      val all = ActivityLog.findAllByUserType(id, 11)
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 

  def getByDish(dishId: Long, userId: Long) = Action { 
    implicit request => {
      Logger.info("calling getlikers (getByDish) dishId:" + dishId + "  userId:" + userId)
      val all = Friend.findDishLikers(List(dishId), userId)
      Logger.info("getlikers returns " + all.size)
      Ok(Json.prettyPrint(Json.toJson(all)))
    }
  } 
  
  def getLikeActivitiesByDish(id: Long) = {
    val activities = ActivityLog.findAll().filter { x => x.activity_type == 11 && x.activity_subtype == id } // TODO this should be optimized on the DB as well
    val allUsers = activities.map(a => User.getFullUser(a.user_id)) //TODO this should be optimized as well
    val all = allUsers.distinct.map(userFull => new UserProfile(userFull.id, userFull.email, userFull.username, 0, 0, 0, 0, null))
    all.foreach { user => user.profileImageURL = Image.findByUser(user.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url } 
    all
  }

  def like(userId: Long, dishId: Long) = Action { 
    implicit request => {
      Logger.info("calling like for dishid:" + dishId + " userid:" + userId)
      val id = ActivityLog.create(userId, 11, dishId, "")
      Logger.info("ActivityLog created - id: "+ id.get + " type: 11 and subtype: " + dishId +  " user: " + userId)
      Ok(Json.toJson(CommonJSONResponse.OK))
    }
  } 
  
  def unlikeSeveral() = Action { 
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id")
      val dish_ids = (request.body.asJson.get \ "dish_ids").as[List[Long]]
      Logger.info("ActivityLog deleted called:  dishId: " + dish_ids +  " user: " + user_id)
      dish_ids.foreach (did => ActivityLog.delete(user_id.as[String].toLong, did))
      Ok(Json.toJson(CommonJSONResponse.OK))
    }
  } 
  
  def unlike(userId: Long, dishIdString: String) = Action { 
    implicit request => {
      val dishId = dishIdString.toLong
      Logger.info("calling unlike/delete for dishid:" + dishId + " userid:" + userId)
      val id = ActivityLog.delete(userId, dishId)
      Logger.info("ActivityLog deleted count: "+ id + " dishId: " + dishId +  " user: " + userId)
      Ok(Json.toJson(CommonJSONResponse.OK))
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
      Ok(Json.toJson(CommonJSONResponse.OK))
    }
  }
}
