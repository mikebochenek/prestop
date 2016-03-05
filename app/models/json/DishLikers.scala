package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DishLikers (friend_id: Long, friend_image_url: String, friend_name: String, friend_city: String, dish_id: Long)

object DishLikers {
  implicit val dishLikersReads = Json.reads[DishLikers]
  implicit val dishLikersWrites = Json.writes[DishLikers]
}