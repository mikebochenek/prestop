package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date

case class PaymentHistory (date: Date, amount: Long, paymentPeriod: String, status: String, 
    stripeChargeID: String, other: String)

object PaymentHistory {
  implicit val paymentHistoryReads = Json.reads[PaymentHistory]
  implicit val paymentHistoryWrites = Json.writes[PaymentHistory]
}