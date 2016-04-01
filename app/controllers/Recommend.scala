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
      Ok(views.html.test(testForm, null, null, null))
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
      val response = Recommendation.recommend(User.getFullUser(id.toLong), 47.385740, 8.518084, null)// longitude.toDouble, latitude.toDouble)
      Ok(views.html.test(testForm, response, Json.prettyPrint(Json.toJson(response)), favs))
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
  
  def getWithFilters(id: Long, longitude: String, latitude: String, maxDistance: Long, minPrice: Double, maxPrice: Double, openNow: Boolean) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude + " filter:" + maxDistance)
      val user = User.getFullUser(id)
      
      val recommendations = Recommendation.recommend(user, longitude.toDouble, latitude.toDouble, maxDistance.toString)
      val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
      ActivityLog.create(user.id, 7, 1, Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
      Ok(json)
    }
  }
  def get(id: Long, longitude: String, latitude: String, filter: String) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude + " filter:" + filter)
      val user = User.getFullUser(id)
      
      val recommendations = Recommendation.recommend(user, longitude.toDouble, latitude.toDouble, filter)
      val json = Json.prettyPrint(Json.toJson(recommendations.dishes.map(a => Json.toJson(a))))
      ActivityLog.create(user.id, 7, 1, Json.toJson(recommendations.dishes.map(x => Json.toJson(x.id))).toString())
      Ok(json)
    }
  }
}
