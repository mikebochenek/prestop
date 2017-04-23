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
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._

import common.Haversine
import common.Recommendation
import models._
import views._

import scala.util.control.Exception.allCatch
import anorm.SqlMappingError


object Recommend extends Controller with Secured  {
  def test() = IsAuthenticated { username =>
    implicit request => {
      Ok(views.html.test(testForm, null, null, null, User.getFullUser(username).get.id.toString, 
          "47.385740", "8.518084", "false", "10", "0.0", "100.0", "100", "", "-1"))
    }
  }

  def testsubmit() = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling recommend test submit")
      val (id, longitude, latitude, openNow, maxdistance, minPrice, maxPrice, maxDishes, 
          avoid, lastDishID) = testForm.bindFromRequest.get
      val fullUser = User.getFullUser(id.toLong)
      var favs = ""
      if (fullUser.settings != null) {
        val settings = Json.parse(fullUser.settings).validate[UserSettings].get
        favs = Json.prettyPrint(Json.toJson(settings.favCuisines))
      }
      val response = Recommendation.recommend(User.getFullUser(id.toLong), longitude.toDouble, latitude.toDouble, maxdistance.toDouble, 
          minPrice.toDouble, maxPrice.toDouble, openNow.toBoolean, lastDishID.toLong, maxDishes.toLong, avoid, null, null, null, null)
      Ok(views.html.test(testForm, response, Json.prettyPrint(Json.toJson(response)), favs, id, 
          longitude, latitude, openNow, maxdistance, minPrice, maxPrice, maxDishes, avoid, lastDishID))
    }
  }
  
  val testForm = Form(
    tuple(
      "id" -> text,
      "longitude" -> text,
      "latitude" -> text,
      "openNow" -> text,
      "maxdistance" -> text,
      "minPrice" -> text,
      "maxPrice" -> text,
      "maxDishes" -> text,
      "avoid" -> text,
      "lastDishID" -> text))  
  
  def getWithFilters(id: Long, longitude: String, latitude: String, maxDistance: Double, minPrice: Double, 
      maxPrice: Double, openNow: Boolean, lastDishID: Long, maxDishes: Long, avoid: String, showOnly: String, 
      showOnlyCuisines: String, sortBy: String) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude 
          + " maxDistance:" + maxDistance + " minPrice:" + minPrice + " maxPrice:" + maxPrice + " openNow:" 
          + openNow + " lastDishID:" + lastDishID + " maxDishes:" + maxDishes + "avoid: " + avoid + " onlyShow:" + showOnly
          + " showOnlyCuisines:" + showOnlyCuisines + " sortBy:" + sortBy)
      try {
        val user = User.getFullUser(id)
        val recommendations = Recommendation.recommend(user, parseLongitude(longitude), parseLatitude(latitude), 
            maxDistance, minPrice, maxPrice, openNow, lastDishID, maxDishes, avoid, null, showOnly, showOnlyCuisines, sortBy)
        val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
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
  def get(id: Long, longitude: String, latitude: String, filter: String) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude + " filter:" + filter)
      val user = User.getFullUser(id)
      
      val recommendations = Recommendation.recommend(user, parseLongitude(longitude), 
          parseLatitude(latitude), 10, 0, 4000.0, false, 0, 100, "", null, null, null, null)
      val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
      ActivityLog.create(user.id, 7, 1, Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
      Ok(json)
    }
  }
  
  def parseLongitude(longitude: String) = {
    if (!(allCatch opt longitude.toDouble).isDefined) {
      47.392 // default longitude
    } else {
      longitude.toDouble
    }
  }
  
  def parseLatitude(latitude: String) = {
    if (!(allCatch opt latitude.toDouble).isDefined) {
      8.5129 // default latitude
    } else {
      latitude.toDouble
    }
  }
  
}
