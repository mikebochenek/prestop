package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger

case class IGLikes (count: Long)

object IGLikes {
  implicit val igLikesReads = Json.reads[IGLikes]
  implicit val igLikesWrites = Json.writes[IGLikes]
}

case class IGNode (caption: String, display_src: String, id: String, likes: IGLikes, 
    name: Option[String], tags: Option[String])

object IGNode {
  implicit val igNodeReads = Json.reads[IGNode]
  implicit val igNodeWrites = Json.writes[IGNode]
}

case class IGMedia (nodes: Seq[IGNode])

object IGMedia {
  implicit val igMediaReads = Json.reads[IGMedia]
  implicit val igMediaWrites = Json.writes[IGMedia]
}

case class IGResponse (status: String, media: IGMedia)

object IGResponse {
  implicit val igResponseReads = Json.reads[IGResponse]
  implicit val igResponseWrites = Json.writes[IGResponse]
  
  def getInstance(s: String) = {
    s match {
      case _ => {
        try {
          Json.parse(s).validate[IGResponse].get
        } catch {
          case e: Exception => {
            Logger.info("failed to parse string s " + e)
            new IGResponse("fake", new IGMedia(Seq.empty[IGNode]))
          }
        }
      }
    }
  }
}
