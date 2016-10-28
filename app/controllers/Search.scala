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

  /**
   * called from /api/searchsuggestions
   */
  def suggest(keyword: String, id: Long) = Action {
    implicit request => {
      ActivityLog.create(id, ActivityLog.TYPE_SEARCH_SUGGEST, 0, keyword)
      val popular = Tag.findAllPopular(Tag.TYPE_INGREDIENTS)
      
      if (keyword.length < 2) {
        val popularSearches = popular.take(10).map { x => SearchSuggestion(x.en_text.get, x.count.get + " dishes") }
        Ok(Json.prettyPrint(Json.toJson(popularSearches)))
      } else {
        val matches = popular.filter { x => x.en_text.get.toLowerCase.startsWith(keyword.toLowerCase.trim) }
          .take(10).map { x => SearchSuggestion(x.en_text.get, x.count.get + " dishes") }
        Ok(Json.prettyPrint(Json.toJson(matches)))
      }
    }
  }

  /**
   * called from /api/search
   */
  def searchWithFilters(keyword: String, id: Long, longitude: String, latitude: String, 
      maxDistance: Double, minPrice: Double, maxPrice: Double, openNow: Boolean, lastDishID: Long, 
      maxDishes: Long, avoid: String) = Action {
    implicit request => {
      Logger.info("calling Search with keyword: " + keyword + " id:" + id + " longitude:" + longitude 
          + " latitude:" + latitude + " maxDistance:" + maxDistance + " minPrice:" + minPrice + " maxPrice:" 
          + maxPrice + " openNow:" + openNow + " lastDishID:" + lastDishID + " maxDishes:" + maxDishes)
      try {
        val user = User.getFullUser(id)
        val recommendations = common.Recommendation.recommend(user, Recommend.parseLongitude(longitude), 
            Recommend.parseLatitude(latitude), maxDistance, minPrice, maxPrice, openNow, lastDishID, maxDishes, avoid, keyword)
        val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
        ActivityLog.create(user.id, ActivityLog.TYPE_SEARCH, lastDishID, keyword + "---" 
            + Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
            
        //TODO we would also need this, for infinite scrolling to work... but its corrupting recommend scrolling etc!!
        ActivityLog.create(user.id, 7, lastDishID, Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
            
        Ok(json)
      } catch {
        case e: Exception => {
          Logger.error("Recommend.getWithFilters(..)", e)
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
