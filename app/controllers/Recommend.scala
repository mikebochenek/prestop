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

object Recommend extends Controller with Secured  {
  def test() = IsAuthenticated { username =>
    implicit request => {
      Ok(views.html.test(testForm, null, null, null, "1", "47.385740", "8.518084", "", "10", ""))
    }
  }

  def testsubmit() = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling recommend test submit")
      val (id, longitude, latitude, jsonoptions, maxdistance, time) = testForm.bindFromRequest.get
      val fullUser = User.getFullUser(id.toLong)
      var favs = ""
      if (fullUser.settings != null) {
        val settings = Json.parse(fullUser.settings).validate[UserSettings].get
        favs = Json.prettyPrint(Json.toJson(settings.favCuisines))
      }
      val response = Recommendation.recommend(User.getFullUser(id.toLong), longitude.toDouble, latitude.toDouble, maxdistance.toDouble, 0, 4000.0, false, 0)
      //testForm.fill(User.getFullUser(id.toLong).id.toString, "47.385740", "8.518084", "", "10", "")
      Ok(views.html.test(testForm, response, Json.prettyPrint(Json.toJson(response)), favs, id, longitude, latitude, jsonoptions, maxdistance, time))
    }
  }
  
  val testForm = Form(
    tuple(
      "id" -> text,
      "longitude" -> text,
      "latitude" -> text,
      "jsonoptions" -> text,
      "maxdistance" -> text,
      "time" -> text))  
  
  def getWithFilters(id: Long, longitude: String, latitude: String, maxDistance: Double, minPrice: Double, maxPrice: Double, openNow: Boolean, lastDishID: Long) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude + " maxDistance:" + maxDistance
          + " minPrice:" + minPrice + " maxPrice:" + maxPrice + " openNow:" + openNow + " lastDishID:" + lastDishID)
      val user = User.getFullUser(id)
      
      val recommendations = Recommendation.recommend(user, longitude.toDouble, latitude.toDouble, maxDistance, minPrice, maxPrice, openNow, lastDishID)
      val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
      ActivityLog.create(user.id, 7, lastDishID, Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
      Ok(json)
    }
  }
  def get(id: Long, longitude: String, latitude: String, filter: String) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude + " filter:" + filter)
      val user = User.getFullUser(id)
      
      val recommendations = Recommendation.recommend(user, longitude.toDouble, latitude.toDouble, 10, 0, 4000.0, false, 0)
      val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
      ActivityLog.create(user.id, 7, 1, Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
      Ok(json)
    }
  }
}
