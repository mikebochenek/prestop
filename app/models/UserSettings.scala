package models

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

case class UserSettings(id: Long, language: String, skipDailyEmail: Boolean, timeZone: String)

object UserSettings {

  implicit val userSettingsReads = Json.reads[UserSettings]
  implicit val userSettingsWrites = Json.writes[UserSettings]

}