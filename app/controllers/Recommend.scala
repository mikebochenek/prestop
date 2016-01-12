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
import models._
import views._

object Recommend extends Controller {
  def get(id: Long, longitude: String, latitude: String, filter: String) = Action {
    implicit request => {
      Logger.info("calling Recommend.get with id:" + id + " longitude:" + longitude + " latitude:" + latitude + " filter:" + filter)
      val user = null //TODO need something like User.findById(id)
      
      Ok(Json.prettyPrint(Json.toJson(recommend(user, longitude.toDouble, latitude.toDouble).dishes.map(a => Json.toJson(a)))))
    }
  }
  
  //47.385740, 8.518084 coordinates for Zurich Hardbrucke
  
  val maxdist = 50000 //TODO this should be more like .8
  def recommend(user: User, longitude: Double, latitude: Double) = {
    val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)
    // http://stackoverflow.com/questions/2925041/how-to-convert-a-seqa-to-a-mapint-a-using-a-value-of-a-as-the-key-in-the-ma
    //TODO we can not iterate in a dumb for-loop because this would not scale
    //TODO ideally, we would read the restaurants only once in a while..
    
    val priceMin = 0
    val priceMax = Double.MaxValue
    
    val dishes = Dish.findAll().filter { x => within(maxdist, restaurants, x.restaurant_id, longitude, latitude) }
      //.filter {x => (priceMax >= x.price && priceMin >= x.price) }    
    val r = new Recommendations(dishes);
    r
  }
  
  def within(max: Double, restaurants: Map[Long, Restaurant], id: Long, longitude: Double, latitude: Double) = {
    // http://www.cis.upenn.edu/~matuszek/cis554-2011/Pages/scalas-option-type.html
    restaurants.get(id) match {
      case Some(f) => Haversine.haversine(f.latitude, f.longitude, longitude, latitude) < max
      case None => false
    }
  }
}
