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

object RecommendationUtils {
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
      val ft = from.replace('.',':').replaceAll(",", "").split(":")
      val tt = to.replace('.',':').replaceAll(",", "").split(":")
      val toHour = tt(0).toLong match { 
        case 0 => 24
        case default => tt(0).toLong
      } 
      val retval = ft(0).toLong <= hour && toHour > hour //TODO should also handle minutes here!
      //Logger.debug("timeWithin: " + from + " - " + to + "  returns -> " + retval)
      retval
    }
    
    //Logger.debug (" day: " + day + " hour:" + hour + " minute:" + minute)

    val lines = s.replaceAll("-"," ").replaceAll("\u2013"," ").replaceAll("bis","-").split("\r\n")
    
    for (str <- lines) {
      val tokens = str.split(" ").filter { x => x.length > 1 }
      
      if (tokens.length == 0) return true //this handles restaurants with empty/null schedule

      val startDay = extractDay(tokens(0))
      if (0 != startDay && tokens.length > 2) {
        if (tokens.length > 4 && isTime(tokens(1)) && isTime(tokens(2)) && isTime(tokens(3)) && isTime(tokens(4))) {
          //Logger.debug("case 1+ .... one day with two time ranges")
          if (day == startDay && (timeWithin(tokens(1), tokens(2)) || timeWithin(tokens(3), tokens(4)))) {
            return true
          }
        } else if (isTime(tokens(1)) && isTime(tokens(2))) {
          //Logger.debug("case 1 .... one day")
          if (day == startDay && timeWithin(tokens(1), tokens(2))) {
            return true
          }
        } else if (tokens.length > 5 && 0 != extractDay(tokens(1)) && isTime(tokens(2)) && isTime(tokens(3)) && isTime(tokens(4)) && isTime(tokens(5))) {
          if (day >= startDay && day <= extractDay(tokens(1)) && (timeWithin(tokens(2), tokens(3)) || timeWithin(tokens(4), tokens(5)))) {
            return true
          }
          //Logger.debug("case 2+ ... day range with two time ranges")
        } else if (0 != extractDay(tokens(1)) && isTime(tokens(2)) && isTime(tokens(3))) {
          if (day >= startDay && day <= extractDay(tokens(1)) && (timeWithin(tokens(2), tokens(3)))) {
            return true
          }
          //Logger.debug("case 2 .... day range - day:" + day + " startday:" + startDay + " endDay:" + extractDay(tokens(1)))
        } else {
          //Logger.info(" .........checkSchedule impossible flow")
          tokens.foreach{t => Logger.debug(t)}
        }
      } else {
        //Logger.info(" .......checkSchedule impossible flow #2")
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
  def isTime(t: String) = { timePattern.findAllIn(t.replace('.',':')).hasNext }
  
  
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