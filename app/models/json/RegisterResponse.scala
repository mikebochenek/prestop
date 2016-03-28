package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class RegisterResponse (status: String, user_id: Long)

object RegisterResponse {
  implicit val registerResponseReads = Json.reads[RegisterResponse]
  implicit val registerResponseWrites = Json.writes[RegisterResponse]
}