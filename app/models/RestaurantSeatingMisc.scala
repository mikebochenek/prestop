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

case class RestaurantSeatingMisc(var reservationsPhone: Option[String],
    var reservationsEmail: Option[String])
    
object RestaurantSeatingMisc {
  def default() = { new RestaurantSeatingMisc(Option(""), Option("")) }
  
  implicit val restaurantSeatingMiscReads = Json.reads[RestaurantSeatingMisc]
  implicit val restaurantSeatingMiscWrites = Json.writes[RestaurantSeatingMisc]
}

