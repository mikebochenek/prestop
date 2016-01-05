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

/**
 * mysql> describe tag;
 * +------------+--------------+------+-----+-------------------+----------------+
 * | Field      | Type         | Null | Key | Default           | Extra          |
 * +------------+--------------+------+-----+-------------------+----------------+
 * | id         | int(11)      | NO   | PRI | NULL              | auto_increment |
 * | owner      | int(11)      | YES  | MUL | NULL              |                |
 * | tag        | varchar(255) | YES  |     | NULL              |                |
 * | createdate | timestamp    | NO   |     | CURRENT_TIMESTAMP |                |
 * | deleted    | tinyint(1)   | YES  |     | NULL              |                |
 * +------------+--------------+------+-----+-------------------+----------------+
 * 5 rows in set (0.03 sec)
 */
case class Tag(id: Long, owner: Long, tag: String, createdate: Date, deleted: Boolean)

object Tag {

  val simple = {
    get[Long]("tag.id") ~
      get[Long]("tag.owner") ~
      get[String]("tag.tag") ~
      get[Date]("tag.createdate") ~
      get[Boolean]("tag.deleted") map {
        case id ~ owner ~ tag ~ createdate ~ deleted => Tag(id, owner, tag, createdate, deleted)
      }
  }

  def findAll(owner: Long): Seq[Tag] = {
    DB.withConnection { implicit connection =>
      SQL("select id, owner, tag, createdate, deleted from tag where owner = {owner}").on(
        'owner -> owner).as(Tag.simple *)
    }
  }

  def create(tag: Tag): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into tag (owner, tag, deleted) values (
          {owner}, {tag}, {deleted}
          )
        """).on(
          'owner -> tag.owner,
          'tag -> tag.tag,
          'deleted -> 0).executeInsert()
    }
  }

  implicit val tagReads = Json.reads[Tag]
  implicit val tagWrites = Json.writes[Tag]

}

