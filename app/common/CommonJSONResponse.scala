package models

import play.api.db._
import play.api.Play.current
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger
import anorm._
import anorm.SqlParser._
import java.util.Date
import scala.collection.mutable.MutableList

case class CommonJSONResponse(status: String)

case class ErrorJSONResponse(status: String, detail: String)

object CommonJSONResponse {
  implicit val jsonResponseReads = Json.reads[CommonJSONResponse]
  implicit val jsonResponseWrites = Json.writes[CommonJSONResponse]
  
  val OK = new CommonJSONResponse("OK")
}

object ErrorJSONResponse {
  implicit val errorJsonResponseReads = Json.reads[ErrorJSONResponse]
  implicit val errorJsonResponseWrites = Json.writes[ErrorJSONResponse]
}
