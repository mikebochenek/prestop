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
import play.api.Logger

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

  def findByRef(id: Long, status: Int): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL("""select t.id, t.name, t.en_text, t.de_text, t.it_text, t.fr_text, t.status, t.lastupdate 
             from tag t 
             join tagref tr on t.id = tr.tagid
             where tr.refid = {id} and tr.status = {status}
          """).on('id -> id, 'status -> status).as(Tag.simple *)
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
          insert into tag (name, en_text, de_text, it_text, fr_text, status, lastupdate) values (
          {name}, {en_text}, {de_text}, {it_text}, {fr_text}, {status}, {lastupdate}
          )
        """).on(
          'name -> tag.name,
          'en_text -> tag.en_text,
          'de_text -> tag.de_text,
          'it_text -> tag.it_text,
          'fr_text -> tag.fr_text,
          'lastupdate -> new Date(),
          'status -> tag.status).executeInsert()
    }
  }

  def updateTags(id: Long, tags: String, status: Int) = {
	  val tagsArray = tags.split(",")
		val oldtags = Tag.findByRef(id.toLong, status).map(_.name)
		val alltags = Tag.findAll
    
    // add new tags 
		for (tag <- tagsArray) {
		  if (!oldtags.contains(tag.trim) && tag.trim.length > 0) {
		    alltags.find(_.name.equals(tag.trim)) match {
					case Some(f) => TagRef.create(new TagRef(-1, f.id, id, status, null))
					case None => { 
            // instead of ignoring the tag, we create it!
            val newId = Tag.create(new Tag(-1, tag.trim, Some(tag.trim), Some(tag.trim), Some(null), Some(null), status, new Date()))
            Logger.warn("new tag created:" + tag + " with newId=" + newId)
            TagRef.create(new TagRef(-1, newId.getOrElse(0), id, status, null))
          }
			  }
			}
		}
    
    // we should also remove tags which are not there anymore
    for (tag <- oldtags) {
      if (!tags.contains(tag.trim)) { // NB tagsArray is messy because sometimes it has blanks, and sometimes not
        val tagid = Tag.findAll().find(_.name.equals(tag)).getOrElse(null).id
        TagRef.deletesoftly(tagid, id)
      }
    }
  }
        
  implicit val tagReads = Json.reads[Tag]
  implicit val tagWrites = Json.writes[Tag]

}

