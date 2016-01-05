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
 * mysql> describe maillog;
 * +------------+------------+------+-----+-------------------+----------------+
 * | Field      | Type       | Null | Key | Default           | Extra          |
 * +------------+------------+------+-----+-------------------+----------------+
 * | id         | int(11)    | NO   | PRI | NULL              | auto_increment |
 * | recipient  | int(11)    | YES  |     | NULL              |                |
 * | subject    | text       | YES  |     | NULL              |                |
 * | content    | text       | YES  |     | NULL              |                |
 * | createdate | timestamp  | NO   |     | CURRENT_TIMESTAMP |                |
 * | status     | tinyint(1) | YES  |     | NULL              |                |
 * +------------+------------+------+-----+-------------------+----------------+
 * 6 rows in set (0.01 sec)
 */
case class MailLog(id: Long, recipient: Long, content: String, subject: String, createdate: Date, status: Int)

object MailLog {
  val simple = {
    get[Long]("maillog.id") ~
      get[Long]("maillog.recipient") ~
      get[String]("maillog.content") ~
      get[String]("maillog.subject") ~
      get[Date]("maillog.createdate") ~
      get[Int]("maillog.status") map {
        case id ~ recipient ~ content ~ subject ~ createdate ~ status => MailLog(id, recipient, content, subject, createdate, status)
      }
  }

  def create(ml: MailLog): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into maillog (recipient, content, subject, createdate, status) values (
          {recipient}, {content}, {subject}, {createdate}, {status}
          )
        """).on(
          'recipient -> ml.recipient,
          'content -> ml.content,
          'subject -> ml.subject,
          'createdate -> new Date(),
          'status -> 1).executeInsert()
    }
  }

  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from maillog ").as(scalar[Long].single)
    }
  }
  
  implicit val mailLogReads = Json.reads[MailLog]
  implicit val mailLogWrites = Json.writes[MailLog]

}