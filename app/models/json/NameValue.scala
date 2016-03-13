package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class NameValue (name: String, value: Option[String])

object NameValue {
  implicit val nameValueReads = Json.reads[NameValue]
  implicit val nameValueWrites = Json.writes[NameValue]
}