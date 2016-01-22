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

case class Tag(id: Long, name: String, en_text: Option[String], de_text: Option[String], it_text: Option[String], fr_text: Option[String], status: Int, lastupdate: Date)

object Tag {

  val simple = {
      get[Long]("tag.id") ~
      get[String]("tag.name") ~
      get[Option[String]]("tag.en_text") ~
      get[Option[String]]("tag.de_text") ~
      get[Option[String]]("tag.it_text") ~
      get[Option[String]]("tag.fr_text") ~
      get[Int]("tag.status") ~
      get[Date]("tag.lastupdate") map {
        case id ~ name ~ en_text ~ de_text ~ it_text ~ fr_text ~ status ~ lastupdate => Tag(id, name, en_text, de_text, it_text, fr_text, status, lastupdate)
      }
  }

  def findByRef(id: Long): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL("""select t.id, t.name, t.en_text, t.de_text, t.it_text, t.fr_text, t.status, t.lastupdate 
             from tag t 
             join tagref tr on t.id = tr.tagid
             where tr.refid = {id}
          """).on('id -> id).as(Tag.simple *)
    }
  }

  def findAll(): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL("""select t.id, t.name, t.en_text, t.de_text, t.it_text, t.fr_text, t.status, t.lastupdate 
             from tag t 
          """).on().as(Tag.simple *)
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

  def updateTags(id: Long, tags: String) = {
	  val tagsArray = tags.split(",")
		val oldtags = Tag.findByRef(id.toLong).map(_.name)
		val alltags = Tag.findAll
		for (tag <- tagsArray) {
		  if (!oldtags.contains(tag.trim)) {
		    alltags.find(_.name.equals(tag.trim)) match {
					case Some(f) => TagRef.create(new TagRef(-1, f.id, id.toLong, 0, null))
					case None => println (tag + ".. tag ignored...")
			  }
			}
		}
    
    //TODO we should also remove tags which are not there anymore
  }
        
  implicit val tagReads = Json.reads[Tag]
  implicit val tagWrites = Json.writes[Tag]

}

