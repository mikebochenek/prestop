package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class FriendSuggestion (user_id: Long, friend_image_url: String, facebook_id: String, friend_name: String, friend_phone: String)

object FriendSuggestion {
  implicit val friendSuggestionReads = Json.reads[FriendSuggestion]
  implicit val friendSuggestionWrites = Json.writes[FriendSuggestion]
}