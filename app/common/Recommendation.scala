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
      .filter { x => within(maxDistance, restaurants, x.restaurant_id, longitude, latitude) } // filter by distance
      .filter { x => (maxPrice >= x.price && minPrice <= x.price) } // filter by price
      .filter { x => !openNow || checkOpenTime(restaurants, x.restaurant_id)} // filter out restaurants currently closed
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
  def checkOpenTime(restaurants: Map[Long, Restaurant], id: Long) = {
    restaurants.get(id) match {
      case Some(f) => { 
        Logger.debug(" checking restaurant schedule: " + f.schedule)
        f.schedule.isEmpty || f.schedule.length == 0 || checkSchedule(f.schedule)
      }
      case None => true
    }
  }
  
  val timezone = TimeZone.getTimeZone("Europe/Copenhagen")
  def checkSchedule(s: String) : Boolean = {
    val calendar = Calendar.getInstance(timezone)
    val day = SUNDAY == calendar.get(DAY_OF_WEEK) match {
      case true => 7
      case default => calendar.get(DAY_OF_WEEK) - 1
    }
    val hour = calendar.get(HOUR_OF_DAY)
    val minute = calendar.get(MINUTE)
    
    def timeWithin(from: String, to: String) = {
      val ft = from.replaceAll(",", "").split(":")
      val tt = to.replaceAll(",", "").split(":")
      val retval = ft(0).toLong <= hour && tt(0).toLong > hour //TODO should also handle minutes here!
      Logger.debug("timeWithin: " + from + " - " + to + "  returns -> " + retval)
      retval
    }
    
    Logger.debug (" day: " + day + " hour:" + hour + " minute:" + minute)

    val lines = s.replaceAll("-"," ").replaceAll("\u2013"," ").replaceAll("bis","-").split("\r\n")
    
    for (str <- lines) {
      val tokens = str.split(" ").filter { x => x.length > 1 }
      
      if (tokens.length == 0) return true //this handles restaurants with empty/null schedule

      val startDay = extractDay(tokens(0))
      if (0 != startDay && tokens.length > 2) {
        if (tokens.length > 4 && isTime(tokens(1)) && isTime(tokens(2)) && isTime(tokens(3)) && isTime(tokens(4))) {
          Logger.debug("case 1+ .... one day with two time ranges")
          if (day == startDay && (timeWithin(tokens(1), tokens(2)) || timeWithin(tokens(3), tokens(4)))) {
            return true
          }
        } else if (isTime(tokens(1)) && isTime(tokens(2))) {
          Logger.debug("case 1 .... one day")
          if (day == startDay && timeWithin(tokens(1), tokens(2))) {
            return true
          }
        } else if (tokens.length > 5 && 0 != extractDay(tokens(1)) && isTime(tokens(2)) && isTime(tokens(3)) && isTime(tokens(4)) && isTime(tokens(5))) {
          if (day >= startDay && day <= extractDay(tokens(1)) && (timeWithin(tokens(2), tokens(3)) || timeWithin(tokens(4), tokens(5)))) {
            return true
          }
          Logger.debug("case 2+ ... day range with two time ranges")
        } else if (0 != extractDay(tokens(1)) && isTime(tokens(2)) && isTime(tokens(3))) {
          if (day >= startDay && day <= extractDay(tokens(1)) && (timeWithin(tokens(2), tokens(3)))) {
            return true
          }
          Logger.debug("case 2 .... day range - day:" + day + " startday:" + startDay + " endDay:" + extractDay(tokens(1)))
        } else {
          Logger.info(" .........checkSchedule impossible flow")
          tokens.foreach{t => Logger.debug(t)}
        }
      } else {
        Logger.info(" .......checkSchedule impossible flow #2")
        tokens.foreach{t => Logger.debug(t)}
      }
    }
    
    false
  }


  def extractDay(s: String) = {
    
    def extractDayNumber(t: String) = {
      t.toLowerCase.take(2) match {
        case "mo" | "mon" | "montag" | "monday" => MONDAY
        case "di" | "die" | "dienstag" | "tu" | "tue" | "tuesday" => TUESDAY
        case "mi" | "mitt" | "mittwoch" | "we" | "wed" | "wednesday" => WEDNESDAY
        case "do" | "donn" | "donnerstag" | "th" | "thur" | "thurs" | "thursday" => THURSDAY
        case "fr" | "frei" | "freitag" | "fri" | "friday" => FRIDAY
        case "sa" | "sam" | "samstag" | "sat" | "saturday" => SATURDAY
        case "so" | "son" | "sonntag" | "su" | "sun" | "sunday" => SUNDAY
        case default => 0
      }
    }
    
    extractDayNumber(s) match {
      case SUNDAY => 7
      case default => extractDayNumber(s) - 1
    }
  }
  
  val timePattern = "([01]?[0-9]|2[0-3]):[0-5][0-9]".r
  def isTime(t: String) = { timePattern.findAllIn(t).hasNext }
  
  
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