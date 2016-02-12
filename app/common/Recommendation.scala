package common

import models._
import play.api.Logger
import models.RecommendationItem
import scala.collection.mutable.ArraySeq
import scala.collection.mutable.MutableList
import java.text.DecimalFormat
import play.api.libs.json._
import play.api.libs.functional.syntax._
import controllers.Activities

object Recommendation {
  //47.385740, 8.518084 coordinates for Zurich Hardbrucke
  //47.411875, 8.548024 Zurich Oerlikon Neudorfstrasse 23 
  //47.356842, 8.514578 Zurich Uetlibergstrasse 231
  //46.953082, 7.446915 Bern
  
  val maxdist = 10 //maximum allowable distance in km
  val priceMin = 0
  val priceMax = 40.0

  def recommend(user: UserFull, longitude: Double, latitude: Double, options: String) = {
    val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)
    // http://stackoverflow.com/questions/2925041/how-to-convert-a-seqa-to-a-mapint-a-using-a-value-of-a-as-the-key-in-the-ma
    
    val likedDishes = Recommendations.getLikedDishes(user.id)
    
    //TODO we can not iterate in a dumb for-loop because this would not scale
    //TODO ideally, we would read the restaurants only once in a while..
    
    
    var desiredWidth = 750L
    if (user.settings != null) {
      desiredWidth = Json.parse(user.settings).validate[UserSettings].get.screenWidth
      if (!Image.resolutions.contains(desiredWidth)) {
        desiredWidth = 750L
      }
    }
        
        
    val dishes = Dish.findAll()
      .filter { x => within(maxdist, restaurants, x.restaurant_id, longitude, latitude) } // filter by distance
      .filter { x => (priceMax >= x.price && priceMin <= x.price) } // filter by price
    
    val result = new Recommendations(MutableList.empty);
    
    
    
    for (dish <- dishes) {
      val friendLikedDishURLs = Activities.getLikeActivitiesByDish(dish.id).map(x => x.profileImageURL)
      //Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url  :: Nil
      
      val r = restaurants.get(dish.restaurant_id).head
      val ri = new RecommendationItem(dish.id, makePriceString(dish.price), dish.name, dish.greenScore, 
        Image.findByDish(dish.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
        makeDistanceString(Haversine.haversine(r.latitude, r.longitude, latitude, longitude)),
        Tag.findByRef(dish.id, 11).map(_.name),
        r.name, Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
        friendLikedDishURLs,
        Tag.findByRef(dish.id, 34).map(_.name),
        Tag.findByRef(dish.id, 35).map(_.name),
        Tag.findByRef(dish.id, 36).map(_.name))
      result.dishes += ri
    }
    result
  }
  
  /**
   * check if restaurant location is within the maximum specified distance
   */
  def within(max: Double, restaurants: Map[Long, Restaurant], id: Long, longitude: Double, latitude: Double) = {
    // http://www.cis.upenn.edu/~matuszek/cis554-2011/Pages/scalas-option-type.html
    restaurants.get(id) match {
      case Some(f) =>
        val distance = Haversine.haversine(f.latitude, f.longitude, latitude, longitude)
        val iswithin = distance < max
        Logger.debug("restaurant: " + f.id + " " + iswithin + " because distance: " + distance + " inputs:" + (f.latitude + " " + f.longitude + " " + longitude + " " + latitude))
        iswithin
      case None => false
    }
  }  
  
  /**
   * check if the restaurant is open
   * (but how to handle situations when someone is booking a restaurant for next Friday?)
   */
  
  val distanceFormat = new DecimalFormat("#.#")
  def makeDistanceString(d: Double) = {
    if (d > 15) {
      d.toLong + " km"
    } else if (d > 1) {
      distanceFormat.format(d) + " km"  
    } else {
      val distance50s = (d * 20).toLong
      ((distance50s / 20.0) * 1000).toLong + " m"
    }
  }
  
  val priceFormat = new DecimalFormat("#.00")
  def makePriceString(p: Double) = {
    priceFormat.format(p)// + " CHF"
  }
}