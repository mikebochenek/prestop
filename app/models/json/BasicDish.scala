
package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class BasicDish (id: Long, name: String, restaurantName: String)

object BasicDish {
  implicit val basicDishReads = Json.reads[BasicDish]
  implicit val basicDishWrites = Json.writes[BasicDish]
}