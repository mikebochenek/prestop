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
import java.util.{ Calendar, GregorianCalendar, TimeZone }
import Calendar.{ DAY_OF_WEEK, HOUR_OF_DAY, MINUTE, SUNDAY, SATURDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY }

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
    val random = new java.util.Random
    val dishesAlreadyRecommendedActivities = ActivityLog.findRecentByUserType(user.id, 7).reverse
    
    def getDishesAlreadyRecommended() :SortedSet[Long] = {
      val dar = SortedSet[Long]()
      
      if (lastDishID <= 0) return dar

      var _prevLastDishID = lastDishID
      for (actDish <- dishesAlreadyRecommendedActivities) {
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
    restaurants.foreach {case(key, r) => r.cuisines =  Tag.findByRef(r.id, 21).map(_.name) } //TODO this must be optimized and cached?
    
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
      .filter { x => RecommendationUtils.within(maxDistance, restaurants, x.restaurant_id, longitude, latitude) } // filter by distance
      .filter { x => (maxPrice >= x.price && minPrice <= x.price) } // filter by price
      .filter { x => !openNow || RecommendationUtils.checkOpenTime(restaurants, x.restaurant_id)} // filter out restaurants currently closed
      .filter { x => !dishesAlreadyRecommended.contains(x.id) } // filter out dishes already recommended (list would be empty if lastDishID is 0 or null)

    val allDietTags = Tag.findByRefList((dishes.map { x => x.id }).toList, Tag.TYPE_DIET)
    val allIngredientTags = Tag.findByRefList((dishes.map { x => x.id }).toList, 11)
    val allLikes = ActivityLog.findAllByUserType(user.id, 11)
    val dishLikers = Friend.findDishLikers((dishes.map { x => x.id }).toList, user.id)  
    val result = new Recommendations(MutableList.empty);
    
    for (dish <- dishes) {
      val like = !(allLikes.find { x => x.activity_subtype == dish.id }.isEmpty)
      
      val r = restaurants.get(dish.restaurant_id).head
      
      var score = random.nextDouble / 100 // one hack could be to score += random(0.01 to 0.09)
      userSettings.favCuisines.foreach { fav => if (r.cuisines.contains(fav.tag)) score += (fav.rating.get * 0.2) } 

      //TODO score does not take into account original dish ratings (from user setup!!)
      
      val dishDietTags = allDietTags.filter { x => x.refid == dish.id }.map(_.name) //Tag.findByRef(dish.id, Tag.TYPE_DIET ).map(_.name)
      userSettings.preferToAvoid.get.foreach { avoid => if (dishDietTags.contains(avoid.tag)) score -= (avoid.rating.get * 1.5) }
      
      allIngredientTags.filter { x => x.refid == dish.id }.foreach { ingredient => if (likedDishes.count { y => y.tagId == ingredient.tagid } > 0) {
        val count = likedDishes.count { y => y.tagId == ingredient.tagid }
        Logger.debug("it looks like user: " + user.id + "  liked: " + ingredient.tagid + "," + ingredient.name + 
            " " + count + " times, and dish: " + dish.id + "," + dish.name.take(15) + " contains it")
        score += (0.1 * count)
      }}
      
      val thisDishLikersCount = dishLikers.filter { x => x.dish_id == dish.id}.size
      if (thisDishLikersCount > 0) {
        Logger.debug ("dish: " + dish.id + "," + dish.name.take(15) + " was liked by " + thisDishLikersCount + " friends")
        score +=  thisDishLikersCount * 0.4       
      }
     
      val ri = new RecommendationItem(dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, like, 0.0, 
        null, null, null,
        RecommendationUtils.makeDistanceString(Haversine.haversine(r.latitude, r.longitude, latitude, longitude)),
        null, r.id, r.name, null, null, dishDietTags, null, null, score)
      
      result.dishes += ri
    }
    
    val sortedResult = result.dishes.sortBy(_.score).reverse
    
    // after we sort, we can skip the dishes which were already shown (recently?) to the user
    var startIdx = 0
    if (!dishesAlreadyRecommendedActivities.isEmpty) {
      val a = dishesAlreadyRecommendedActivities.head.activity_details // only consider very last set of dishes recommended
      if (a.contains("[") && a.indexOf(']') > 0) {
        Logger.debug("___  dishesAlreadyRecommendedActivities : " + a.substring(1, a.indexOf(']')))
        val ids = a.substring(1, a.indexOf(']')).split(",")
        for (i <- 0 until sortedResult.length) {
          if (ids.size > (maxDishes - 5) && ids((maxDishes - 5).toInt).toInt == sortedResult(i).id) { // -5 because iOS performs call to API before user reaches last 5
            startIdx = i
          }
        }
      }
    }
    
    
    // we can also reorganize the dishes: swap if subsequent dishes are from the same restaurant (for the first 100 dishes or so)
    var prevRestaurantID = 0L
    for (i <- 0 until sortedResult.length) {
      if (sortedResult(i).restaurantID == prevRestaurantID) {
        //Logger.debug(" we should probably swap " + i)
        var swapDone = false;
        for (j <- i until sortedResult.length) { // only compare to restaurants after duplicate
          if (!swapDone && sortedResult(i).restaurantID != sortedResult(j).restaurantID) {
            //Logger.debug(" swap " + i + "   "+ j)
            val tmp = sortedResult(i)
            sortedResult.update(i, sortedResult(j))
            sortedResult.update(j, tmp)
            swapDone = true // only swap once
          }
        }
      }
      prevRestaurantID = sortedResult(i).restaurantID 
    }
    
    Logger.debug("------startIdx-------- " + startIdx + "    " + sortedResult.size)
    
    // create circular stream (list) and double the contents, and then extract (slice) based on startIdx - http://stackoverflow.com/questions/3256169/iterating-circular-way
    val sortedResultSubset = Stream.continually(sortedResult.toStream).take(2).flatten.toList.slice(startIdx, startIdx+maxDishes.toInt)
    
    val allSortedDishIDs = sortedResult.map { x => x.id }.toList
    val allMeatOriginTags = Tag.findByRefList(allSortedDishIDs, Tag.TYPE_MEATORIGIN)
    val allDishTypeTags = Tag.findByRefList(allSortedDishIDs, Tag.TYPE_DISHTYPE)

    
    // populate other relevant dish information for display (but only for the maxDishes, not for all!)
    for (r <- sortedResultSubset) {
      val imgUrl = Image.findByDish(r.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      r.url = imgUrl
      r.url_large = imgUrl
      
      val greenscoretags = Tag.findByRef(r.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse("")) // greenscore tags needs the english text
      r.greenScoreTags = greenscoretags
      r.greenScore = Dishes.calculateGreenScore(greenscoretags.size)

      r.ingredients = allIngredientTags.filter { x => x.refid == r.id}.map(_.name)
      r.meatOrigin = allMeatOriginTags.filter { x => x.refid == r.id}.map(_.name) 
      r.dishType = allDishTypeTags.filter{ x => x.refid == r.id}.map(_.name)
      r.friendLikeUrls = dishLikers.filter { x => x.dish_id == r.id && x.friend_image_url != null}.map { y => y.friend_image_url }  //TODO in cases where its null, should we show a default image?
      r.restaurantUrl = Image.findByRestaurant(r.restaurantID).filter{x => x.width.get == 72 && x.status == 1}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
    }
    
    // and lastly, this is how we ensure that we only show dishes with photos
    result.dishes.clear
    result.dishes ++= (sortedResultSubset.filter { x => x.url != null })
    result
  }
}