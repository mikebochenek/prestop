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
import models.json.DishLikers

case class Friend(friend_user_id: Long, user_id: Long, id: Long, status: Int, lastupdate: Date)

object Friend {
  val simple = {
      get[Long]("friend.friend_user_id") ~
      get[Long]("friend.user_id") ~
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

  def update(id: Long, user_id: Long, friend_user_id: Long, status: Int) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          update friend set user_id = {user_id}, friend_user_id = {friend_user_id}, 
          status = {status}, lastupdate = {lastupdate} where id = {id}
        """).on(
          'user_id -> user_id,
          'lastupdate-> new Date(),
          'status -> status,
          'id -> id,
          'friend_user_id -> friend_user_id).executeUpdate()
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

  def findAllFriends(friend_user_id: Long): Seq[Friend] = {
    DB.withConnection { implicit connection =>
      SQL("select user_id, friend_user_id, id, status, lastupdate from friend where friend_user_id = {friend_user_id}"
          + " order by id asc").on('friend_user_id-> friend_user_id).as(Friend.simple *)
    }
  }

  val simpleDishLikers = {
      get[Long]("friend_user_id") ~
      get[String]("url") ~
      get[String]("fullname") ~
      get[Option[String]]("city") ~
      get[Long]("activity_log.activity_subtype") map {
        case friend_user_id ~ url ~ fullname ~ city ~ dish_id => 
          DishLikers(friend_user_id, url, fullname, city.getOrElse(""), dish_id)
      }
  }

  def findDishLikers(dish_id: List[Long], user_id: Long): Seq[DishLikers] = {
    val dishIds = dish_id.mkString(",") 
    Logger.info("findDishLikers user_id: " + user_id + "   dishIds: " + dishIds)
    dish_id.size match {
      case 0 => Seq.empty[DishLikers]
      case default => DB.withConnection { implicit connection =>
        SQL("""
          select distinct f.friend_user_id, i.url, u.fullname, u.city, a.activity_subtype as dish_id from friend f 
          inner join user u on f.friend_user_id = u.id and f.status >= 0
          inner join image i on i.user_id = f.friend_user_id and i.width = 72 and i.status >= 0
          inner join activity_log a on a.user_id = f.friend_user_id and a.activity_type = 11 and a.activity_subtype in (%s)
          where f.user_id = {user_id}
        """.format(dishIds)).on('user_id-> user_id).as(Friend.simpleDishLikers *)
      }
    }
  }
  
  implicit val friendReads = Json.reads[Friend]
  implicit val friendWrites = Json.writes[Friend]

}