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
      Ok(views.html.test(testForm, null, null))
    }
  }

  def testsubmit() = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling recommend test submit")
      val (id, longitude, latitude, jsonoptions, maxdistance, time) = testForm.bindFromRequest.get
      val response = Recommendation.recommend(User.getFullUser(id.toLong), 47.385740, 8.518084, null)// longitude.toDouble, latitude.toDouble)
      Ok(views.html.test(testForm, response, Json.prettyPrint(Json.toJson(response))))
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
  
      
  def get(id: Long, longitude: String, latitude: String, filter: String) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude + " filter:" + filter)
      val user = User.getFullUser(id)
      
      val json = Json.prettyPrint(Json.toJson(Recommendation.recommend(user, longitude.toDouble, latitude.toDouble, filter).dishes.map(a => Json.toJson(a))))
      ActivityLog.create(user.id, 7, 1, json)
      Ok(json)
    }
  }
}
