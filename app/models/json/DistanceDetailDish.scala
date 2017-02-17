package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DistanceDetailDish (id: Long, name: String, desc: String, price: Double, ingredients: String, distance: String, exactDistance: Double)

object DistanceDetailDish {
  implicit val distanceDetailDishReads = Json.reads[DistanceDetailDish]
  implicit val distanceDetailDishWrites = Json.writes[DistanceDetailDish]
}