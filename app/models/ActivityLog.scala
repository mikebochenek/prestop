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

case class ActivityLog(id: Long, user_id: Long, activity_type: Long, activity_subtype: Long,
    activity_details: String, createdate: Date)

object ActivityLog {
  val simple = {
      get[Long]("activity_log.id") ~
      get[Long]("activity_log.user_id") ~
      get[Long]("activity_log.activity_type") ~
      get[Long]("activity_log.activity_subtype") ~
      get[String]("activity_log.activity_details") ~
      get[Date]("activity_log.createdate") map {
        case id ~ user_id ~ activity_type ~ activity_subtype ~ activity_details ~ createdate => 
          ActivityLog(id, user_id, activity_type, activity_subtype, activity_details, createdate)
      }
  }

  def create(user_id: Long, activity_type: Long, activity_subtype: Long, activity_details: String): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into activity_log (user_id, createdate, activity_type, activity_subtype, activity_details) values (
          {user_id}, {createdate}, {activity_type}, {activity_subtype}, {activity_details}
          )
        """).on(
          'user_id -> user_id,
          'createdate -> new Date(),
          'activity_type -> activity_type,
          'activity_subtype -> activity_subtype,
          'activity_details -> activity_details).executeInsert()
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from activity_log").as(scalar[Long].single)
    }
  }

  def findById(username: String, id: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL("select id, user_id, createdate, activity_type, activity_subtype, activity_details from activity_log where id = {id}").on(
        'id -> id).as(ActivityLog.simple *)
    }
  }
  
  def findAll(): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL("select id, user_id, createdate, activity_type, activity_subtype, activity_details from activity_log "
          + " order by id asc").on().as(ActivityLog.simple *)
    }
  }
  
  def findAllByUser(user_id: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL("select id, user_id, createdate, activity_type, activity_subtype, activity_details from activity_log where user_id = {user_id}"
          + " order by id asc").on('user_id -> user_id).as(ActivityLog.simple *)
    }
  }  

  implicit val activityLogReads = Json.reads[ActivityLog]
  implicit val activityLogWrites = Json.writes[ActivityLog]

}