package controllers

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
import scala.collection.mutable.MutableList
import models.ErrorJSONResponse
import models.json.SearchSuggestion

object Search extends Controller with Secured {

  case class SearchTag(tag: String, var count: Long)
  
  def index():Seq[SearchTag] = {
    val startTS = System.currentTimeMillis
    val allDishes = Dish.findAll()
      
    val dishSearchTags = Tag.findDishSearchTags().map { x => SearchTag(x.en_text.get, x.count.get) }
    val cuisineSearchTags = Tag.findCuisineSearchTags().map { x => SearchTag(x.en_text.get, x.count.get) }
      
    val dishNameSearchTags = allDishes.map { x => x.name }.flatMap(_.split(" ").filter { y => y.length() > 3 })
         .toList.groupBy((word: String) => word).mapValues(_.length).map { x => SearchTag(x._1, x._2) }

    Logger.debug("___dishNameSearchTags: " + dishNameSearchTags)

    val restaurantSearchTags = Restaurant.findAll() //TODO and check against dishes for each restaurant..
    
    val unmerged = (dishSearchTags ++ cuisineSearchTags ++ dishNameSearchTags)
    val merged = MutableList.empty[SearchTag]
    for (u <- unmerged) {
      val mfilter = merged.filter { x => u.tag.equalsIgnoreCase(x.tag) }
      if (mfilter.isEmpty) {
        merged += SearchTag(u.tag.toLowerCase, u.count)
      } else {
        mfilter.head.count = mfilter.head.count + u.count
      }
    }
    Logger.info("building search index ... " + (System.currentTimeMillis - startTS) + " ms")
    merged
  }
  
  /**
   * called from /api/searchsuggestions
   */
  def suggest(keyword: String, id: Long) = Action {
    implicit request => {
      ActivityLog.create(id, ActivityLog.TYPE_SEARCH_SUGGEST, 0, keyword)
      
      if (keyword.length < 2) {
        val popularSearches = (index()).sortWith(_.count > _.count).take(10)
           .map { x => SearchSuggestion(x.tag, x.count + " dishes") }
        Ok(Json.prettyPrint(Json.toJson(popularSearches)))
      } else {
        val matches = (index()).sortWith(_.count > _.count)
           .filter { x => x.tag.toLowerCase.contains(keyword.toLowerCase.trim) }
           .take(10)
           .map { x => SearchSuggestion(x.tag, x.count + " dishes") }
        Ok(Json.prettyPrint(Json.toJson(matches)))
      }
    }
  }

  /**
   * called from /api/search
   */
  def searchWithFilters(keyword: String, id: Long, longitude: String, latitude: String, 
      maxDistance: Double, minPrice: Double, maxPrice: Double, openNow: Boolean, lastDishID: Long, 
      maxDishes: Long, avoid: String, showOnly: String, showOnlyCuisines: String, sortBy: String) = Action {
    implicit request => {
      Logger.info("calling Search with keyword: " + keyword + " id:" + id + " longitude:" + longitude 
          + " latitude:" + latitude + " maxDistance:" + maxDistance + " minPrice:" + minPrice + " maxPrice:" 
          + maxPrice + " openNow:" + openNow + " lastDishID:" + lastDishID + " maxDishes:" + maxDishes
          + "avoid: " + avoid + " onlyShow:" + showOnly + " showOnlyCuisines:" + showOnlyCuisines + " sortBy:" + sortBy)
      try {
        val user = User.getFullUser(id)
        val recommendations = common.Recommendation.recommend(user, Recommend.parseLongitude(longitude), 
            Recommend.parseLatitude(latitude), maxDistance, minPrice, maxPrice, openNow, lastDishID, maxDishes, avoid, keyword, showOnly, showOnlyCuisines, sortBy)
        val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
        ActivityLog.create(user.id, ActivityLog.TYPE_SEARCH, lastDishID, keyword + "---" 
            + Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
            
        //TODO we would also need this, for infinite scrolling to work... but its corrupting recommend scrolling etc!!
        ActivityLog.create(user.id, 7, lastDishID, Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
            
        Ok(json)
      } catch {
        case e: Exception => {
          Logger.error("Search.getWithFilters(..)", e)
          if ("SqlMappingError(No rows when expecting a single one)".equalsIgnoreCase(e.getMessage)) {
            Ok(Json.toJson(new ErrorJSONResponse("user does not exist", id+"")))
          } else {
            Ok(Json.toJson(new ErrorJSONResponse("general error", "")))
          }
        }
      }
    }
  }
}
