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

case class Friend(friend_user_id: Long, user_id: Long, id: Long, status: Int, lastupdate: Date)

object Friend {
  val simple = {
      get[Long]("friend.user_id") ~
      get[Long]("friend.friend_user_id") ~
      get[Long]("friend.id") ~
      get[Int]("friend.status") ~
      get[Date]("friend.lastupdate") map {
        case friend_user_id ~ user_id ~ id ~ status ~ lastupdate => 
          Friend(friend_user_id, user_id, id, status, lastupdate)
      }
  }

  def create(user_id: Long, friend_user_id: Long, status: Int): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into friend (user_id, friend_user_id, status, lastupdate) values (
          {user_id}, {friend_user_id}, {status}, {lastupdate}
          )
        """).on(
          'user_id -> user_id,
          'lastupdate-> new Date(),
          'status -> status,
          'friend_user_id -> friend_user_id).executeInsert()
    }
  }
  
  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from friend").as(scalar[Long].single)
    }
  }

  def findById(id: Long): Seq[Friend] = {
    DB.withConnection { implicit connection =>
      SQL("select user_id, friend_user_id, id, status, lastupdate from friend where id = {id}").on(
        'id -> id).as(Friend.simple *)
    }
  }
  
  def findAll(): Seq[Friend] = {
    DB.withConnection { implicit connection =>
      SQL("select user_id, friend_user_id, id, status, lastupdate from friend "
          + " order by id asc").on().as(Friend.simple *)
    }
  }
  
  def findAllByUser(user_id: Long): Seq[Friend] = {
    DB.withConnection { implicit connection =>
      SQL("select user_id, friend_user_id, id, status, lastupdate from friend where user_id = {user_id}"
          + " order by id asc").on('user_id -> user_id).as(Friend.simple *)
    }
  }  

  implicit val friendReads = Json.reads[Friend]
  implicit val friendWrites = Json.writes[Friend]

}