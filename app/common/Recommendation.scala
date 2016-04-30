package common

import models._
import play.api.Logger
import models.RecommendationItem
import scala.collection.mutable.ArraySeq
import scala.collection.mutable.MutableList
import scala.collection.mutable.SortedSet
import java.text.DecimalFormat
import play.api.libs.json._
import play.api.libs.functional.syntax._
import controllers.Activities
import controllers.Dishes
import play.cache.Cache
import java.util.concurrent.Callable
import play.api.libs.json._
import controllers._
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
  
  
  def recommend(user: UserFull, latitude: Double, longitude: Double, maxDistance: Double, minPrice: Double, maxPrice: Double, openNow: Boolean, lastDishID: Long, maxDishes: Long) = {
    
    def getDishesAlreadyRecommended() :SortedSet[Long] = {
      val dar = SortedSet[Long]()
      
      if (lastDishID <= 0) return dar
      
      val dishesAlreadyRecommended = ActivityLog.findAllByUserType(user.id, 7).reverse
      dishesAlreadyRecommended.foreach {x => Logger.debug("___ dishesAlreadyRecommended:  " + x)}

      var _prevLastDishID = lastDishID
      for (actDish <- dishesAlreadyRecommended) {
        val a = actDish.activity_details
        if (a.contains("[") && a.indexOf(']') > 0) {
          Logger.debug("___ part 1 : " + a.substring(1, a.indexOf(']'))  +  " _prevLastDishID: " + _prevLastDishID)
          val ids = a.substring(1, a.indexOf(']')).split(",")
        
          if (ids.contains("" + _prevLastDishID)) {
            ids.foreach { x => if (!"".equals(x)) dar += x.toLong }
            _prevLastDishID = actDish.activity_subtype.toLong //ids.last.toLong
            Logger.debug("___ part 2!: " + ids.mkString(",") +  " new _prevLastDishID: " + _prevLastDishID)
          }
        }
      }

      Logger.info("getDishesAlreadyRecommended() returns: " + dar.mkString(","))
      dar
    }

    
    // http://stackoverflow.com/questions/2925041/how-to-convert-a-seqa-to-a-mapint-a-using-a-value-of-a-as-the-key-in-the-ma
    val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*) //getAllRestaurants()
    restaurants.foreach {case(key, r) => r.cuisines =  Tag.findByRef(r.id, 21).map(_.en_text.get) } //TODO this must be optimized and cached?
    
    val likedDishes = Recommendations.getLikedDishes(user.id)
    
    //TODO we can not iterate in a dumb for-loop because this would not scale
    //TODO ideally, we would read the restaurants only once in a while..
    
    //TODO load favorites cuisines similar to Recommend.testsubmit...
    //TODO or what about a nightly batch job which re-ranks all dishes for all active users?
    
    val userSettings = Settings.getPreviousSettingsSafely(user)
    val desiredWidth = Image.resolutions.contains(userSettings.deviceSWidth.getOrElse(750.0).toLong) match {
      case true => userSettings.deviceSWidth.get.toLong
      case false => 750L
    }

    //Logger.debug ("desiredWidth: " + desiredWidth)
    
    val dishesAlreadyRecommended = getDishesAlreadyRecommended()
    
    val dishes = Dish.findAll() // getAllDishes()
      .filter { x => within(maxDistance, restaurants, x.restaurant_id, longitude, latitude) } // filter by distance
      .filter { x => (maxPrice >= x.price && minPrice <= x.price) } // filter by price
      .filter { x => !dishesAlreadyRecommended.contains(x.id) } // filter out dishes already recommended (list would be empty if lastDishID is 0 or null)
      .take(maxDishes.toInt) 
    
    val result = new Recommendations(MutableList.empty);
    
    for (dish <- dishes) {
      val allLikes = Activities.getLikeActivitiesByDish(dish.id)
      val like = !(allLikes.find { x => x.id == user.id }.isEmpty)
      
      val friendLikedDishURLs = allLikes.map(x => x.profileImageURL).filter { url => url != null }  //TODO in cases where its null, should we show a default image?
      //Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url  :: Nil

      val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.name)
      
      val r = restaurants.get(dish.restaurant_id).head
      
      var score = 0.0
      userSettings.favCuisines.foreach { fav => if (r.cuisines.contains(fav.tag)) score += fav.rating.get } 

      val dishDietTags = Tag.findByRef(dish.id, Tag.TYPE_DIET ).map(_.name)
      userSettings.preferToAvoid.get.foreach { avoid => if (dishDietTags.contains(avoid.tag)) score -= (avoid.rating.get * 1.5) }
      
      val ri = new RecommendationItem(dish.id, makePriceString(dish.price), dish.name, like, Dishes.calculateGreenScore(greenscoretags.size), 
        greenscoretags,
        Image.findByDish(dish.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
        Image.findByDish(dish.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
        makeDistanceString(Haversine.haversine(r.latitude, r.longitude, latitude, longitude)),
        Tag.findByRef(dish.id, 11).map(_.name),
        r.id,
        r.name, Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
        friendLikedDishURLs,
        dishDietTags,
        Tag.findByRef(dish.id, Tag.TYPE_DISHTYPE).map(_.name),
        Tag.findByRef(dish.id, Tag.TYPE_MEATORIGIN).map(_.name), score)
      result.dishes += ri
    }
    
    new Recommendations(result.dishes.sortBy(_.score).reverse)
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
    if (p == 0.0) "" 
    else priceFormat.format(p)// + " CHF"
  }
}