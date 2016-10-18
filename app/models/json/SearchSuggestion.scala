package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SearchSuggestion (keyword: String, count: String)

object SearchSuggestion {
  implicit val searchSuggestionResponseReads = Json.reads[SearchSuggestion]
  implicit val searchSuggestionResponseWrites = Json.writes[SearchSuggestion]
}