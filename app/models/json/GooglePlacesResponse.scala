package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

case class GooglePlacesResponseResultGeometryLocation (lat: Double, lng: Double)

object GooglePlacesResponseResultGeometryLocation {
  implicit val googlePlacesResponseResultGeometryLocationReads = Json.reads[GooglePlacesResponseResultGeometryLocation]
  implicit val googlePlacesResponseResultGeometryLocationWrites = Json.writes[GooglePlacesResponseResultGeometryLocation]
}

case class GooglePlacesResponseResultGeometry (location: GooglePlacesResponseResultGeometryLocation)

object GooglePlacesResponseResultGeometry {
  implicit val googlePlacesResponseResultGeometryReads = Json.reads[GooglePlacesResponseResultGeometry]
  implicit val googlePlacesResponseResultGeometryWrites = Json.writes[GooglePlacesResponseResultGeometry]
}

case class GooglePlacesResponseResult (name: String, international_phone_number: String, website: String, geometry: Option[GooglePlacesResponseResultGeometry])

object GooglePlacesResponseResult {
  implicit val googlePlacesResponseResultReads = Json.reads[GooglePlacesResponseResult]
  implicit val googlePlacesResponseResultWrites = Json.writes[GooglePlacesResponseResult]
}

case class GooglePlacesResponse (result: GooglePlacesResponseResult)

object GooglePlacesResponse {
  implicit val googlePlacesResponseReads = Json.reads[GooglePlacesResponse]
  implicit val googlePlacesResponseWrites = Json.writes[GooglePlacesResponse]
  
  def getInstance(s: String) = {
    s match {
      case _ => {
        try {
          Json.parse(s).validate[GooglePlacesResponse].get
        } catch {
          case e: Exception => {
            Logger.info("failed to parse string s " + e)
            new GooglePlacesResponse(new GooglePlacesResponseResult("","","", None))
          }
        }
      }
    }
  }
}
