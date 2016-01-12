package models

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

case class RecommendationFilter(maxDistance: Long, maxPrice: Double, minPrice: Double, vegetarianOnly: Boolean)

object RecommendationFilter {

  implicit val userSettingsReads = Json.reads[RecommendationFilter]
  implicit val userSettingsWrites = Json.writes[RecommendationFilter]

}