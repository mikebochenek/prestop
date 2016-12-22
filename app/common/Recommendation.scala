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
  
  
  def recommend(user: UserFull, latitude: Double, longitude: Double, maxDistance: Double, minPrice: Double, 
      maxPrice: Double, openNow: Boolean, lastDishID: Long, maxDishes: Long, avoid: String, keyword: String, onlyShow: String) = {
    val random = new java.util.Random
    val dishesAlreadyRecommendedActivities = ActivityLog.findRecentByUserType(user.id, 7).reverse
    
    Logger.info("avoid: " + avoid + "  onlyShow:" + onlyShow)
    
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


    /** Task #404: Bug: sometimes we return a dish which has already been seen (seems to happen a lot) */
    def getDishesRecentlyRecommended() :SortedSet[Long] = {
      val dar = SortedSet[Long]()
      
      if (keyword != null && keyword.trim.length > 0) return dar //because for search, we can return same results
      
      val onlySubset = dishesAlreadyRecommendedActivities.take(5) //TODO
      for (actDish <- onlySubset) {
        val a = actDish.activity_details
        if (a.contains("[") && a.indexOf(']') > 0) {
          val ids = a.substring(1, a.indexOf(']')).split(",")
          ids.foreach { x => if (!"".equals(x)) dar += x.toLong }
        }
      }

      Logger.info("getDishesRecentlyRecommended() returns: " + dar.mkString(","))
      dar
    }
    
    // http://stackoverflow.com/questions/2925041/how-to-convert-a-seqa-to-a-mapint-a-using-a-value-of-a-as-the-key-in-the-ma
    val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*) //getAllRestaurants()
    restaurants.foreach {case(key, r) => r.cuisines =  Tag.findByRef(r.id, 21).map(_.name) } //TODO this must be optimized and cached?
    
    val restaurantSublocations = Restaurant.findAllSublocations
    
    for (rsubloc <- restaurantSublocations) {
      val sublocationDistance = Haversine.haversine(rsubloc.latitude, rsubloc.longitude, latitude, longitude)
      for (r <- restaurants.values) {
        if (r.id == rsubloc.misc.parentRestaurantId.get) { 
          if (Haversine.haversine(r.latitude, r.longitude, latitude, longitude) > sublocationDistance) {
            Logger.info("sublocation " + rsubloc.id + " is closer, so we will swap with parent " + r.id)
            r.latitude = rsubloc.latitude
            r.longitude = rsubloc.longitude
          }
        }
      }
    } //TODO this double for loop should be optimized, especially since I don't need to re-calc the distances each time
    
    val likedDishes = Recommendations.getLikedDishes(user.id)
    
    //TODO we can not iterate in a dumb for-loop because this would not scale
    //TODO ideally, we would read the restaurants only once in a while..
    
    // ? load favorites cuisines similar to Recommend.testsubmit...
    // ? or what about a nightly batch job which re-ranks all dishes for all active users?
    
    val userSettings = Settings.getPreviousSettingsSafely(user)
    val desiredWidth = Image.resolutions.contains(userSettings.deviceSWidth.getOrElse(750.0).toLong) match {
      case true => userSettings.deviceSWidth.get.toLong
      case false => 750L
    }

    //Logger.debug ("desiredWidth: " + desiredWidth)
    
    val dishesAlreadyRecommended = getDishesAlreadyRecommended()
    val dishesRecentlyRecommended = getDishesRecentlyRecommended()
    
    val allDishes = (keyword != null && keyword.trim.length > 0) match {
      case true => {
        val dishesWithKeyword = TagRef.findByENTagText(keyword).map { x => x.refid }
        Logger.debug("dishesWithKeyword ---> " + dishesWithKeyword.size + " --> " + dishesWithKeyword) 
        Dish.findAll.filter { x => dishesWithKeyword.contains(x.id) }
      }
      case false => Dish.findAll
    } // NB: this is a little bit hacky, because we fill allDishes with either all, or already filtered by keyword search
    
    var dishes = allDishes //Dish.findAll() //TODO !!! use local allDishes // getAllDishes()
      .filter { x => RecommendationUtils.within(maxDistance, restaurants, x.restaurant_id, longitude, latitude) } // filter by distance
      .filter { x => (maxPrice >= x.price && minPrice <= x.price) } // filter by price
      .filter { x => !openNow || RecommendationUtils.checkOpenTime(restaurants, x.restaurant_id)} // filter out restaurants currently closed
      .filter { x => !dishesAlreadyRecommended.contains(x.id) } // filter out dishes already recommended (list would be empty if lastDishID is 0 or null)
      .filter { x => x.status != 4 } // filter out dishes in draft mode
      
    if (null != onlyShow && onlyShow.length > 0) {
      val gTags = onlyShow.split(",")
      var dishesShowOnly = MutableList.empty[Long]
      for (_tag <- gTags) {
        val greenTagID = Tag.findAll().filter { x => _tag.equals(x.name) }(0).id
        val _dishesShowOnly = TagRef.findByTag(greenTagID).map { x => x.refid }
        Logger.debug("_dishesShowOnly: " + _dishesShowOnly + "  for " + _tag)
        if (dishesShowOnly.size == 0) {
          dishesShowOnly ++= _dishesShowOnly
        } else {
          dishesShowOnly = dishesShowOnly.intersect(_dishesShowOnly)
        }
      }
      Logger.info("onlyShow: " + onlyShow + "  dishesShowOnly: " + dishesShowOnly)
      dishes = dishes.filter { x => dishesShowOnly.contains(x.id) }
    }
      
    Logger.debug("dishes0.size : " + dishes.size)
    dishes = dishes.size > 50 match {
      case true => dishes.filter { x => !dishesRecentlyRecommended.contains(x.id) } // filter out dishes RECENTLY recommended 
      case false => dishes
    }
      
    val allDietTags = Tag.findByRefList((dishes.map { x => x.id }).toList, Tag.TYPE_DIET)
    val allIngredientTags = Tag.findByRefList((dishes.map { x => x.id }).toList, 11)
    val allLikes = ActivityLog.findAllByUserType(user.id, 11)
    val dishLikers = Friend.findDishLikers((dishes.map { x => x.id }).toList, user.id)  
    val dishImages = Image.findByDishIDs((dishes.map { x => x.id }).toList, desiredWidth, user.id)
    val result = new Recommendations(MutableList.empty);
    
    val sampleDishIngredients = Tag.findByRefList(userSettings.sampleDishLikes.get.map { sample => sample.tag.toLong }.toList, 11)
    
    for (dish <- dishes) {
      val like = !(allLikes.find { x => x.activity_subtype == dish.id }.isEmpty)
      
      val r = restaurants.get(dish.restaurant_id).head
      
      var score = random.nextDouble / 100 // one hack could be to score += random(0.01 to 0.09)
      userSettings.favCuisines.foreach { (fav => if (r.cuisines.contains(fav.tag)) score += (fav.rating.get * settingsFavCuisinesScoreWeight)) } 

      val dishDietTags = allDietTags.filter { x => x.refid == dish.id }.map(_.name) //Tag.findByRef(dish.id, Tag.TYPE_DIET ).map(_.name)
      userSettings.preferToAvoid.get.foreach { (avoid => if (dishDietTags.contains(avoid.tag)) score -= (avoid.rating.get * settingsPreferToAvoidScoreWeight)) }
      
      val thisDishIngredients = allIngredientTags.filter { x => x.refid == dish.id }
      thisDishIngredients.foreach { (ingredient => if (likedDishes.count { y => y.tagId == ingredient.tagid } > 0) {
        val count = likedDishes.count { y => y.tagId == ingredient.tagid }
        Logger.debug("it looks like user: " + user.id + "  liked: " + ingredient.tagid + "," + ingredient.name + 
            " " + count + " times, and dish: " + dish.id + "," + dish.name.take(15) + " contains it")
        score += (likedDishesScoreWeight * count)
      })}

      // score needs take into account original dish ratings (from user setup!)
      thisDishIngredients.foreach { (ingredient => if (sampleDishIngredients.contains(ingredient)) { 
        val count = sampleDishIngredients.count { y => y.tagid == ingredient.tagid } //TODO no distinction between like and really like
        Logger.debug("sampleDishIngredients: it looks like user: " + user.id + "  liked: " + ingredient.tagid + "," + ingredient.name + 
            " " + count + " times, and dish: " + dish.id + "," + dish.name.take(15) + " contains it")
        score += (sampleDishesScoreWeight * count)
      })}
      
      val thisDishLikersCount = dishLikers.filter { x => x.dish_id == dish.id}.size
      if (thisDishLikersCount > 0) {
        Logger.debug ("dish: " + dish.id + "," + dish.name.take(15) + " was liked by " + thisDishLikersCount + " friends")
        score +=  thisDishLikersCount * friendLikesScoreWeight       
      }
      
      val ri = new RecommendationItem(dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, dish.source, dish.description.getOrElse(""), like, 0.0, 
        null, null, null,
        RecommendationUtils.makeDistanceString(Haversine.haversine(r.latitude, r.longitude, latitude, longitude)),
        null, r.id, r.name, r.city + ", " + r.misc.country.getOrElse(""), null, null, dishDietTags, null, null, score)

      ri.score += scoreDistance(ri.distance)
      
      var avoidDish = false
      dishDietTags.foreach { t => if (avoid.contains(t)) avoidDish = true }
     
      if (!avoidDish) {
        result.dishes += ri
      }
    }
    
    var sortedResult = result.dishes.sortBy(_.score).reverse
    
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
      val imgUrl = dishImages.filter { x => x.dish_id == r.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
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
    result.dishes ++= (sortedResultSubset.filter { x => x.url != null }.distinct)
    result
  }

  val friendLikesScoreWeight = 0.4 // for each friend who had liked this dish, increase score by X

  val sampleDishesScoreWeight = 0.1 // for each sample dish ingredient that matches this dish, increase score by X

  val likedDishesScoreWeight = 0.1  // for each liked dish ingredient that matches this dish, increase score by X

  val settingsPreferToAvoidScoreWeight = 1.5 // for each ingredient (from prefer to avoid) that matches this dish, penalize score by X

  val settingsFavCuisinesScoreWeight = 0.2 // for each favorite cuisine that matches this restaurant, increase by X
  
  def scoreDistance(distStr: String) : Double = {
    val d = distStr.split(" ")
    val dist = d(1) match {
      case "km" => d(0).toDouble * 1000
      case "m" => d(0).toDouble
      case default => 0
    }
    
    if (dist > 100000) return -4.0 // if distance is more than 100km, penalize score by -4
    if (dist > 10000) return -2.0  // if distance is more than  10km, penalize score by -2
    if (dist > 1000) return -0.1   // if distance is more than   1km, penalize score by -0.1
    
    return 0.0
  }
}