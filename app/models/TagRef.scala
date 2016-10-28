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

case class TagRef(id: Long, tagid: Long, refid: Long, status: Int, lastupdate: Date)

object TagRef {

  val simple = {
      get[Long]("tagref.id") ~
      get[Long]("tagref.tagid") ~
      get[Long]("tagref.refid") ~
      get[Int]("tagref.status") ~
      get[Date]("tagref.lastupdate") map {
        case id ~ tagid ~ refid ~ status ~ lastupdate => TagRef(id, tagid, refid, status, lastupdate)
      }
  }

  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from tagref ").as(scalar[Long].single)
    }
  }
  
  def findAll(): Seq[TagRef] = {
    DB.withConnection { implicit connection =>
      SQL("select id, tagid, refid, status, lastupdate from tagref ").as(TagRef.simple *)
    }
  }

  def findByRef(refid: Long): Seq[TagRef] = {
    DB.withConnection { implicit connection =>
      SQL("select id, tagid, refid, status, lastupdate from tagref where status > 0 and refid = {refid}").on(
        'refid -> refid).as(TagRef.simple *)
    }
  }

  def findByTag(tagid: Long): Seq[TagRef] = {
    DB.withConnection { implicit connection =>
      SQL("select id, tagid, refid, status, lastupdate from tagref where status > 0 and tagid = {tagid}").on(
        'tagid -> tagid).as(TagRef.simple *)
    }
  }

  def findByENTagText(en_text: String): Seq[TagRef] = {
    DB.withConnection { implicit connection =>
      SQL("""select tr.id, tr.tagid, tr.refid, tr.status, tr.lastupdate from tagref tr 
             join tag t on t.id = tr.tagid 
             where tr.status > 0 and t.en_text LIKE {en_text+"%"}""").on(
        'en_text -> en_text.toLowerCase.trim).as(TagRef.simple *)
    }
  }
  

  def deletesoftly(tagid: Long, refid: Long) = {
    DB.withConnection { implicit connection =>
      SQL("update tagref set status = -1 where refid = {refid} and tagid = {tagid}").on(
          'refid -> refid, 'tagid -> tagid).executeUpdate
    }
  }
  
  def create(tagref: TagRef): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into tagref (tagid, refid, status, lastupdate) values (
          {tagid}, {refid}, {status}, {lastupdate}
          )
        """).on(
          'tagid -> tagref.tagid,
          'refid -> tagref.refid,
          'lastupdate -> new Date(),
          'status -> tagref.status).executeInsert()
    }
  }

  implicit val tagReads = Json.reads[TagRef]
  implicit val tagWrites = Json.writes[TagRef]
}
