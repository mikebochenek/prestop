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
    var preferToAvoid: Option[MutableList[Cuisine]], var sampleDishLikes: Option[MutableList[Cuisine]])

object UserSettings {
  implicit val userSettingsReads = Json.reads[UserSettings]
  implicit val userSettingsWrites = Json.writes[UserSettings]
}

