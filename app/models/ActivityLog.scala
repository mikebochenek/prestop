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
  
  def delete(user_id: Long, activity_subtype: Long): Long = {
    DB.withConnection { implicit connection =>
      SQL("delete from activity_log where user_id = {user_id} and activity_subtype = {activity_subtype}").on(
          'user_id -> user_id, 'activity_subtype -> activity_subtype).executeUpdate
    }
  }

  def delete(user_id: Long): Long = {
    DB.withConnection { implicit connection =>
      SQL("delete from activity_log where user_id = {user_id} ").on(
          'user_id -> user_id).executeUpdate
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from activity_log").as(scalar[Long].single)
    }
  }
  
  val selectSQL = "select id, user_id, createdate, activity_type, activity_subtype, activity_details from activity_log "

  def findById(username: String, id: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where id = {id}").on(
        'id -> id).as(ActivityLog.simple *)
    }
  }
  
  def findAll(): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where activity_type <> 7 order by id asc").on().as(ActivityLog.simple *)
    }
  }
  
  def findAllByUser(user_id: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + "where user_id = {user_id} order by id asc").on('user_id -> user_id).as(ActivityLog.simple *)
    }
  }  

  def findAllByUserType(user_id: Long, atype: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where user_id = {user_id} and activity_type = {atype}"
          + " order by id asc").on('user_id -> user_id, 'atype -> atype).as(ActivityLog.simple *)
    }
  }  
  
  def findAllBySubType(atype: Long, subtype: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where activity_type = {atype}"
          + " and activity_subtype = {subtype} "
          + " order by id asc").on('atype -> atype, 'subtype -> subtype).as(ActivityLog.simple *)
    }
  }

  def findAllByType(interval: Long, atype: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where activity_type = {atype}"
          + " and createdate >= DATE(NOW()) - INTERVAL " + interval + " DAY "
          + " order by id asc").on('atype -> atype).as(ActivityLog.simple *)
    }
  }  

  def findRecentByUserType(user_id: Long, atype: Long): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " where user_id = {user_id} and activity_type = {atype}"
          + " and createdate >= DATE(NOW()) - INTERVAL 5 DAY "
          + " order by id asc").on('user_id -> user_id, 'atype -> atype).as(ActivityLog.simple *)
    }
  }  

  def findRecentActivities(): Seq[ActivityLog] = {
    DB.withConnection { implicit connection =>
      SQL(selectSQL + " order by id desc limit 10").as(ActivityLog.simple *)
    }
  }  
  
  def findRecentStats(atype: Long, interval: Long): Seq[ActivityLogUserStats] = {
    DB.withConnection { implicit connection =>
      SQL("select u.email, al.user_id, u.username, u.phone, al.activity_type, count(*) as ccount "
       + " from activity_log al, user u "
       + " where u.id = al.user_id  "
       + " and al.activity_type = {atype} "
       + " and al.createdate >= (NOW() - INTERVAL " + interval + " DAY) "
       + " group by u.email, al.user_id, al.activity_type order by ccount desc "
      ).on('atype -> atype).as(ActivityLogUserStats.simple *)
    }
  }  
  
  def averageDailyDishViewers(days: Long): java.math.BigDecimal = {
    DB.withConnection { implicit connection =>
      SQL("select avg(daily_users) from ( "
           + " select count(*) as daily_users, d from ( "
           + "   SELECT user_id, DATE(`createdate`) as d, count(*)"
           + "   FROM activity_log "
           + "   where activity_type = 7 and createdate > (now() - interval 300 day)"
           + "   GROUP BY DATE(`createdate`), user_id"
           + "  ) as TT group by d order by d desc"
           + ") as TTT").as(scalar[java.math.BigDecimal].single)
    }
  }
  
  val TYPE_PAYMENT_AUDIT = 5
  val TYPE_RECOMMEND_API_CALL = 7
  val TYPE_RESTAURANT_DETAILS_API_CALL = 9
  val TYPE_DISH_LIKE = 11
  val TYPE_DISH_UPLOAD = 13
  val TYPE_SEARCH_SUGGEST = 15
  val TYPE_SEARCH = 17
  val TYPE_LOGIN_ATTEMPT = 19
  
  implicit val activityLogReads = Json.reads[ActivityLog]
  implicit val activityLogWrites = Json.writes[ActivityLog]
}

case class ActivityLogUserStats(email: String, user_id: Long, username: String, phone: String, activity_type: Long, count: Long)

object ActivityLogUserStats {
  val simple = {
      get[Option[String]]("user.email") ~
      get[Long]("activity_log.user_id") ~
      get[String]("user.username") ~
      get[Option[String]]("user.phone") ~
      get[Long]("activity_log.activity_type") ~
      get[Long]("ccount") map {
        case email ~ user_id ~ username ~ phone ~ activity_type ~ ccount => 
          ActivityLogUserStats(email.getOrElse(null), user_id, username, phone.getOrElse(null), activity_type, ccount)
      }
  }
  implicit val activityLogUSReads = Json.reads[ActivityLogUserStats]
  implicit val activityLogUSWrites = Json.writes[ActivityLogUserStats]
}