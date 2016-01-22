package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import scala.language.postfixOps
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Tag(id: Long, name: String, en_text: String, de_text: String, it_text: String, fr_text: String, status: Int, lastupdate: Date)

object Tag {

  val simple = {
      get[Long]("tag.id") ~
      get[String]("tag.name") ~
      get[String]("tag.en_text") ~
      get[String]("tag.de_text") ~
      get[String]("tag.it_text") ~
      get[String]("tag.fr_text") ~
      get[Int]("tag.status") ~
      get[Date]("tag.lastupdate") map {
        case id ~ name ~ en_text ~ de_text ~ it_text ~ fr_text ~ status ~ lastupdate => Tag(id, name, en_text, de_text, it_text, fr_text, status, lastupdate)
      }
  }

  def findByRef(id: Long): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL("select id, name, en_text, de_text, it_text, fr_text, status, lastupdate from tag where id = {d}").on(
        'id -> id).as(Tag.simple *)
    }
  }

  def create(tag: Tag): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into tag (en_text, de_text, it_text, fr_text, status, lastupdate) values (
          {en_text}, {de_text}, {it_text}, {fr_text}, {status}, {lastupdate}
          )
        """).on(
          'en_text -> tag.en_text,
          'de_text -> tag.de_text,
          'it_text -> tag.it_text,
          'fr_text -> tag.fr_text,
          'lastupdate -> new Date(),
          'status -> tag.status).executeInsert()
    }
  }

  implicit val tagReads = Json.reads[Tag]
  implicit val tagWrites = Json.writes[Tag]

}

