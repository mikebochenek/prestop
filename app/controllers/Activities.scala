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

  /**
   * called from /api/getlikes/:id
   */
  def getLikesByUser(id: Long) = Action { 
    implicit request => {
	    Logger.info("calling get activities (getByUser) id:" + id)
      
      //TODO all the below is way too similar to Recommendation.recommend and should be refactored into a common method
      val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)

      var result = DishLikesContainer(MutableList.empty);

      val activities = ActivityLog.findAllByUserType(id, 11).filter { x => x.activity_type == 11} //needs to be optimized
      Logger.info("activities " + activities + " size:" + activities.size)

      val dishIDList = (activities.map { x => x.activity_subtype}).toList
      val dishLikers = Friend.findDishLikers(dishIDList, id)  
      val dishImages172 = Image.findByDishIDs(dishIDList, 172, id)
      val dishImages750 = Image.findByDishIDs(dishIDList, 750, id)
      val allDietTags = Tag.findByRefList(dishIDList, Tag.TYPE_DIET)
      val allIngredientTags = Tag.findByRefList(dishIDList, 11)

      for (ac <- activities) {
        val dish = Dish.findById(null, ac.activity_subtype)(0)
        
        if (dish.status >= 0 && !restaurants.get(dish.restaurant_id).isEmpty) {
          val allLikes = Activities.getLikeActivitiesByDish(dish.id)
          val like = true
       
          val friendLikedDishURLs = dishLikers.filter { x => x.dish_id == dish.id && x.friend_image_url != null}
            .map { y => y.friend_image_url }

          val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse(""))
        
          val r = restaurants.get(dish.restaurant_id).head
          val ri = new DishLikes(id, dish.id, dish.id, RecommendationUtils.makePriceString(dish.price), 
              dish.name, dish.source, dish.description.getOrElse(""), like, Dishes.calculateGreenScore(greenscoretags.size), 
            greenscoretags,
            dishImages172.filter{x => x.dish_id == dish.id}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
            dishImages750.filter{x => x.dish_id == dish.id}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
            null,
            allIngredientTags.filter{x => x.refid == dish.id}.map(_.name),
            r.id,
            r.name, r.city + ", " + r.misc.country.getOrElse(""), 
            Image.findByRestaurant(r.id).filter{x => x.width.get == 72 && x.status == 1}
              .headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
            friendLikedDishURLs,
            allDietTags.filter{x => x.refid == dish.id}.map(_.name),
            Tag.findByRef(dish.id, 35).map(_.name))
          result.likes += ri
        }
      }
      
      Ok(Json.prettyPrint(Json.toJson(result.likes.map(a => Json.toJson(a)))))
	  }
  } 
  
  /**
   * called from /api/activities/:id
   */
  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling get activities (getByUser) id:" + id)
      val all = ActivityLog.findAllByUserType(id, 11)
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(all)))))
    }
  } 

  /**
   * called from /api/getlikers/:dishId,:userId
   */
  def getByDish(dishId: Long, userId: Long) = Action { 
    implicit request => {
      Logger.info("calling getlikers (getByDish) dishId:" + dishId + "  userId:" + userId)
      val all = Friend.findDishLikers(List(dishId), userId)
      Logger.info("getlikers returns " + all.size)
      Ok(Json.prettyPrint(Json.toJson(all)))
    }
  } 
  
  def getLikeActivitiesByDish(id: Long) = {
    val activities = ActivityLog.findAll().filter { x => x.activity_type == 11 && x.activity_subtype == id } 
      // TODO this should be optimized on the DB as well
    val allUsers = activities.map(a => User.getFullUser(a.user_id)) //TODO this should be optimized as well
    val all = allUsers.distinct.map(userFull => new UserProfile(userFull.id, userFull.email, userFull.username, 0, 0, 0, 0, null))
    all.foreach { user => user.profileImageURL = Image.findByUser(user.id).filter{x => x.width.get == 72}.headOption
      .getOrElse(Image.blankImage).asInstanceOf[Image].url } 
    all
  }

  /**
   * called from /api/like/:userId,:dishId
   */
  def like(userId: Long, dishId: Long) = Action { 
    implicit request => {
      Logger.info("calling like for dishid:" + dishId + " userid:" + userId)
      val id = ActivityLog.create(userId, 11, dishId, "")
      Logger.info("ActivityLog created - id: "+ id.get + " type: 11 and subtype: " + dishId +  " user: " + userId)
      Ok(Json.toJson(CommonJSONResponse.OK))
    }
  } 
  
  /**
   * called from /api/unlike (as an http post)
   */
  def unlikeSeveral() = Action { 
    implicit request => {
      Thread.sleep(800)
      val user_id = (request.body.asJson.get \ "user_id")
      val dish_ids = (request.body.asJson.get \ "dish_ids").as[List[Long]]
      Logger.info("ActivityLog deleted called:  dishId: " + dish_ids +  " user: " + user_id)
      dish_ids.foreach (did => ActivityLog.delete(user_id.as[String].toLong, did))
      Ok(Json.toJson(CommonJSONResponse.OK))
    }
  } 
  
  /**
   * called from /api/unlike/:userId,:dishId
   */
  def unlike(userId: Long, dishIdString: String) = Action { 
    implicit request => {
      val dishId = dishIdString.toLong
      Logger.info("calling unlike/delete for dishid:" + dishId + " userid:" + userId)
      val id = ActivityLog.delete(userId, dishId)
      Logger.info("ActivityLog deleted count: "+ id + " dishId: " + dishId +  " user: " + userId)
      Ok(Json.toJson(CommonJSONResponse.OK))
    }
  } 

  /**
   * called from /api/activities/create
   */
  def create() = Action {
    implicit request => {
      val user_id = (request.body.asJson.get \ "user_id")
      val activity_type = (request.body.asJson.get \ "activity_type")
      val activity_subtype = (request.body.asJson.get \ "activity_subtype")
      val activity_details = (request.body.asJson.get \ "activity_details")
      val id = ActivityLog.create(user_id.as[String].toLong, activity_type.as[String].toLong, 
          activity_subtype.as[String].toLong, activity_details.as[String])
      Logger.info("ActivityLog created - id: "+ id.get + " type:" + activity_type.as[String] 
        + " subtype: " + activity_subtype.as[String] +  " user: " + user_id.as[String])
      Ok(Json.toJson(CommonJSONResponse.OK))
    }
  }
}
