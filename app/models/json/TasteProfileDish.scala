package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class TasteProfileDish (dish_id: Long, url: String, price: String, name: String, description: String)

object TasteProfileDish {
  implicit val tasteProfileDishReads = Json.reads[TasteProfileDish]
  implicit val tasteProfileDishWrites = Json.writes[TasteProfileDish]
}