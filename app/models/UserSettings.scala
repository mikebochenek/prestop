package models

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger
import scala.collection.mutable.MutableList

case class Cuisine(tag: String, rating: Option[Int])

object Cuisine {
  implicit val cuisineReads = Json.reads[Cuisine]
  implicit val cuisineWrites = Json.writes[Cuisine]
}

case class UserSettings(id: Long, var language: String, skipDailyEmail: Boolean, 
    timeZone: String, screenWidth: Long, var favCuisines: MutableList[Cuisine], 
    var preferToAvoid: Option[MutableList[Cuisine]], var sampleDishLikes: Option[MutableList[Cuisine]],
    var deviceOS: Option[String], var deviceSWidth: Option[Long], var deviceLang: Option[String])
    
object UserSettings {
  def default(id: Long) = { new UserSettings(id, "en_US", false, "", 0, MutableList.empty[Cuisine], Option(MutableList.empty[Cuisine]), Option(MutableList.empty[Cuisine]), Option(""), Option(0), Option("")) }
  
  implicit val userSettingsReads = Json.reads[UserSettings]
  implicit val userSettingsWrites = Json.writes[UserSettings]
}

