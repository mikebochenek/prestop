package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DishLikes (dish_id: Long, url: String, user_id: Long)

object DishLikes {
  implicit val dishLikesReads = Json.reads[DishLikes]
  implicit val dishLikesWrites = Json.writes[DishLikes]
}