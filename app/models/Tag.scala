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

case class TagRefSimple(tagid: Long, name: String, status: Int, refid: Long)

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

  val optimized = {
      get[Long]("tag.id") ~
      get[String]("tag.name") ~
      get[Int]("tag.status") ~
      get[Long]("tagref.refid") map {
        case id ~ name ~ status ~ refid => TagRefSimple(id, name, status, refid)
      }
  }

  def findByRefStatusList(id: Long, status: List[Int]): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " join tagref tr on t.id = tr.tagid where tr.refid = {id} and tr.status in (%s)".format(status.mkString(",")))
        .on('id -> id).as(Tag.simple *)
    }
  }
  
  def findByRef(id: Long, status: Int): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " join tagref tr on t.id = tr.tagid where tr.refid = {id} and tr.status = {status}")
        .on('id -> id, 'status -> status).as(Tag.simple *)
    }
  }
  
  def findByRefList(id: List[Long], status: Int): Seq[TagRefSimple] = {
    val ids = id.mkString(",")
    Logger.info("findByRefList: " + id.size + " --> " + ids)
    id.size match {
      case 0 => Seq.empty[TagRefSimple]
      case default => DB.withConnection { implicit connection =>
        SQL(" select t.id, t.name, t.status, tr.refid from tag t join tagref tr on t.id = tr.tagid where t.status >= 0 and tr.refid in (%s) and tr.status = {status}".format(ids))
          .on('status -> status).as(Tag.optimized *)
      }
    }
  }

  val selectSQL = "select t.id, t.name, t.en_text, t.de_text, t.it_text, t.fr_text, t.status, t.lastupdate from tag t "

  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from tag ").as(scalar[Long].single)
    }
  }
  
  def findAll(): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " order by t.name ").on().as(Tag.simple *)
    }
  }
  
  def findAllPopular(status: Long): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL("select t.*, (select count(*) from presto.tagref tr where t.id = tr.tagid) as c from presto.tag t where t.status = {status} order by c desc, t.name ")
      .on('status -> status).as(Tag.simple *)
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

  def update(id: Int, name: String, en: String, de: String, it: String, fr: String, status: Long) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update tag set name = {name}, en_text = {en_text}, de_text = {de_text},
         it_text = {it_text}, fr_text = {fr_text}, status = {status} where 
         id = {id} 
        """).on(
          'id -> id,
          'name -> name,
          'en_text -> en,
          'de_text -> de,
          'it_text -> it,
          'fr_text -> fr,
          'status -> status).executeUpdate
    }
  }  

  def updateTags(id: Long, tags: String, status: Int): Unit = {
    updateTags(id, tags, Seq(status))
  }
  
  def updateTags(id: Long, tags: String, status: Seq[Int]): Unit = {
	  val tagsArray = tags.split(",")
		val oldtags = Tag.findByRefStatusList(id.toLong, status.toList).map(_.name) 
		val alltags = Tag.findAll
    
    // add new tags 
		for (tag <- tagsArray) {
		  if (!oldtags.contains(tag.trim.toLowerCase) && tag.trim.length > 0) {
		    alltags.find(_.name.equals(tag.trim.toLowerCase)) match {
			    case Some(f) => {
			      TagRef.create(new TagRef(-1, f.id, id, f.status, null))
			      if (f.status < 0) {
			        Tag.update(f.id.toInt, f.name, f.en_text.getOrElse(""), f.de_text.getOrElse(""), 
			            f.it_text.getOrElse(""), f.fr_text.getOrElse(""), status(0)) // this doesn't do anything since refactoring - Sept 17
			      }
			    }
			    case None => { 
            // instead of ignoring the tag, we create it (OR reactivate with if status < 0 above!)
            val newId = Tag.create(new Tag(-1, tag.trim.toLowerCase, Some(tag.trim.toLowerCase), Some(null), Some(null), Some(null), status(0), new Date()))
            Logger.warn("new tag created:" + tag + " with newId=" + newId)
            TagRef.create(new TagRef(-1, newId.getOrElse(0), id, status(0), null))
          }
			  }
		  }
		}
    
    // we should also remove tags which are not there anymore
    for (tag <- oldtags) {
      if (!tags.contains(tag.trim.toLowerCase)) { // NB tagsArray is messy because sometimes it has blanks, and sometimes not
        val tagid = Tag.findAll().find(_.name.equals(tag)).getOrElse(null).id
        TagRef.deletesoftly(tagid, id)
      }
    }
  }
        
  implicit val tagReads = Json.reads[Tag]
  implicit val tagWrites = Json.writes[Tag]

  val TYPE_INGREDIENTS = 11
  val TYPE_PAYMENTS = 12
  val TYPE_CUISINE = 21
  val TYPE_GREENSCORE = 31
  val TYPE_DIET = 34
  val TYPE_DISHTYPE = 35
  val TYPE_MEATORIGIN = 36
  val TYPE_INTRO_TEXTS = 41
}

