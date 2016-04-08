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
import controllers.Dishes
import play.cache.Cache
import java.util.concurrent.Callable
import play.api.libs.json._
import com.fasterxml.jackson.core.JsonParseException

object Recommendation {
  //47.385740, 8.518084 coordinates for Zurich Hardbrucke
  //47.411875, 8.548024 Zurich Oerlikon Neudorfstrasse 23 
  //47.356842, 8.514578 Zurich Uetlibergstrasse 231
  //46.953082, 7.446915 Bern
    
  { //TODO ... but isn't it kinda wrong that I need to place these in the cache myself
    Cache.set("allrestaurants", Map(Restaurant.findAll map { a => a.id -> a}: _*))
    Cache.set("alldishes", Dish.findAll())
  }

  def getAllRestaurants() = {
    //Cache.get("allrestaurants").asInstanceOf[Map[Long,models.Restaurant]]
    Cache.getOrElse("allrestaurants", new Callable[Map[Long,models.Restaurant]] { def call() = Map(Restaurant.findAll map { a => a.id -> a}: _*) }, 100)
  }
  
  def getAllDishes() = {
    //Cache.get("alldishes").asInstanceOf[Seq[Dish]]
    Cache.getOrElse("alldishes", new Callable[Seq[Dish]] { def call() = Dish.findAll()}, 5)
  }
  
  def recommend(user: UserFull, latitude: Double, longitude: Double, maxDistance: Double, minPrice: Double, maxPrice: Double, openNow: Boolean, lastDishID: Long) = {
    val restaurants = getAllRestaurants()//Map(Restaurant.findAll map { a => a.id -> a}: _*) //getAllRestaurants()
    // http://stackoverflow.com/questions/2925041/how-to-convert-a-seqa-to-a-mapint-a-using-a-value-of-a-as-the-key-in-the-ma
    
    val likedDishes = Recommendations.getLikedDishes(user.id)
    
    //TODO we can not iterate in a dumb for-loop because this would not scale
    //TODO ideally, we would read the restaurants only once in a while..
    
    //TODO load favorites cuisines similar to Recommend.testsubmit...
    //TODO or what about a nightly batch job which re-ranks all dishes for all active users?
    
    var desiredWidth = 750L
    if (user.settings != null) {
      desiredWidth = Json.parse(user.settings).validate[UserSettings].get.screenWidth
      if (!Image.resolutions.contains(desiredWidth)) {
        desiredWidth = 750L
      }
    }
        
        
    val dishes = getAllDishes()//Dish.findAll()
      .filter { x => within(maxDistance, restaurants, x.restaurant_id, longitude, latitude) } // filter by distance
      .filter { x => (maxPrice >= x.price && minPrice <= x.price) } // filter by price
      .take(100) //TODO for now, limit to 100 dishes..
    
    val result = new Recommendations(MutableList.empty);
    
    
    
    for (dish <- dishes) {
      val allLikes = Activities.getLikeActivitiesByDish(dish.id)
      val like = !(allLikes.find { x => x.id == user.id }.isEmpty)
      
      val friendLikedDishURLs = allLikes.map(x => x.profileImageURL).filter { url => url != null }  //TODO in cases where its null, should we show a default image?
      //Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url  :: Nil

      val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.name)
      
      val r = restaurants.get(dish.restaurant_id).head
      val ri = new RecommendationItem(dish.id, makePriceString(dish.price), dish.name, like, Dishes.calculateGreenScore(greenscoretags.size), 
        greenscoretags,
        Image.findByDish(dish.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
        Image.findByDish(dish.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
        makeDistanceString(Haversine.haversine(r.latitude, r.longitude, latitude, longitude)),
        Tag.findByRef(dish.id, 11).map(_.name),
        r.id,
        r.name, Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
        friendLikedDishURLs,
        Tag.findByRef(dish.id, Tag.TYPE_DIET ).map(_.name),
        Tag.findByRef(dish.id, Tag.TYPE_DISHTYPE).map(_.name),
        Tag.findByRef(dish.id, Tag.TYPE_MEATORIGIN).map(_.name))
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
        //Logger.debug("restaurant: " + f.id + " " + iswithin + " because distance: " + distance + " inputs:" + (f.latitude + " " + f.longitude + " " + longitude + " " + latitude))
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