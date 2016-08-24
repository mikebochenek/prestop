package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import scala.language.postfixOps
import java.util.Date
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Calendar

/**
 * mysql> describe user
 * -> ;
 * +---------------+--------------+------+-----+---------+-------+
 * | Field         | Type         | Null | Key | Default | Extra |
 * +---------------+--------------+------+-----+---------+-------+
 * | id            | bigint(20)   | NO   | PRI | NULL    |       |
 * | createdate    | datetime     | YES  |     | NULL    |       |
 * | lastlogindate | datetime     | YES  |     | NULL    |       |
 * | deleted       | bit(1)       | YES  |     | NULL    |       |
 * | password      | varchar(255) | YES  |     | NULL    |       |
 * | settings      | longtext     | YES  |     | NULL    |       |
 * | username      | varchar(255) | YES  |     | NULL    |       |
 * | email         | varchar(255) | YES  |     | NULL    |       |
 * | type          | varchar(255) | YES  |     | NULL    |       |
 * | openidtoken   | varchar(255) | YES  |     | NULL    |       |
 * +---------------+--------------+------+-----+---------+-------+
 * 10 rows in set (0.00 sec)
 *
 */

case class User(id: Long, email: String, username: String, password: String)

case class UserFull(id: Long, createdate: Date, lastlogindate: Option[Date], deleted: Boolean, 
    var password: String, settings: String, email: String, username: String, ttype: String,
    openidtoken: String, fullname: String, city: String, state: String, country: String, var phone: String)

case class UserProfile(id: Long, email: String, username: String, 
    var following: Int, var followers: Int, var likes: Int, var reservations: Int, var profileImageURL: String)

case class UserStat(id: Long, createdate: Date, email: String, username: String, fullname: String,
    phone: String, allTime: Long, threeMonths: Long, month: Long, week: Long)
    
object User {

  /**
   * Parse a User from a ResultSet
   */
  val simple = {
    get[Long]("user.id") ~
      get[String]("user.email") ~
      get[String]("user.username") ~
      get[String]("user.password") map {
        case id ~ email ~ username ~ password => User(id, email, username, password)
      }
  }

  val stat = {
    get[Long]("user.id") ~
      get[Date]("user.createdate") ~
      get[String]("user.email") ~
      get[String]("user.username") ~
      get[Option[String]]("user.fullname") ~
      get[Option[String]]("user.phone") ~
      get[Long]("allTime") ~
      get[Long]("threeMonths") ~
      get[Long]("month") ~
      get[Long]("week") map {
        case id ~ createdate ~ email ~ username ~ fullname ~ phone ~ allTime ~ threeMonths ~ month ~ week => 
          UserStat(id, createdate, email, username, fullname.getOrElse(null), phone.getOrElse(null), allTime, threeMonths, month, week)
      }
  }
  
  val all = { 
    get[Long]("user.id") ~
      get[Option[Date]]("user.createdate") ~
      get[Option[Date]]("user.lastlogindate") ~
      get[Option[Boolean]]("user.deleted") ~
      get[String]("user.password") ~
      get[Option[String]]("user.settings") ~
      get[String]("user.email") ~
      get[String]("user.username") ~
      get[Option[String]]("user.type") ~
      get[Option[String]]("user.openidtoken") ~
      get[Option[String]]("user.fullname") ~
      get[Option[String]]("user.city") ~
      get[Option[String]]("user.state") ~
      get[Option[String]]("user.country") ~
      get[Option[String]]("user.phone") map {
        case id ~ createdate ~ lastlogindate ~ deleted ~ password ~ settings ~ email ~ username ~ ttype ~ openidtoken ~ fullname ~ city ~ state ~ country ~ phone => 
          UserFull(id, createdate.getOrElse(null), lastlogindate, deleted.getOrElse(false), 
              password, settings.getOrElse(null), email, username, ttype.getOrElse(""), openidtoken.getOrElse(null),
              fullname.getOrElse(null), city.getOrElse(null), state.getOrElse(null), country.getOrElse(null), phone.getOrElse(null))
      }
  }
  
  val columns = " id, createdate, lastlogindate, deleted, password, settings, email, username, type, openidtoken, fullname, city, state, country, phone "
  
  def getFullUser(email: String): Option[UserFull] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from user where email = {email} order by id desc limit 1").on(
        'email -> email).as(User.all.singleOpt)
    }
  }
  
  def getFullUser(id: Long): UserFull = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from user where id = {id}").on(
        'id -> id).as(User.all.single)
    }
  }

  def getFullUserByUsername(username: String): Option[UserFull] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from user where username = {username} order by id desc limit 1").on(
        'username -> username).as(User.all.singleOpt)
    }
  }

  def getFullUserByPhone(phone: String): Seq[UserFull] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from user where phone = {phone}").on(
        'phone -> phone).as(User.all. *)
    }
  }
  
  def findByEmail(email: String): User = {
    DB.withConnection { implicit connection =>
      SQL("select id, email, username, password from user where email = {email}").on(
        'email -> email).as(User.simple.single)
    }
  }

  def findAll: Seq[UserFull] = {
    DB.withConnection { implicit connection =>
      SQL("select " + columns + " from user").as(User.all. *)
    }
  }
  
  def getUsersStats: Seq[UserStat] = {
    DB.withConnection { implicit connection =>
      SQL("""
           select id, createdate, email, username, fullname, phone, 
             (select count(*) from activity_log al where activity_type = 7 and al.user_id = u.id) as allTime, 
             (select count(*) from activity_log al where activity_type = 7 and al.user_id = u.id and createdate >= DATE(NOW()) - INTERVAL 90 DAY) as threeMonths, 
             (select count(*) from activity_log al where activity_type = 7 and al.user_id = u.id and createdate >= DATE(NOW()) - INTERVAL 30 DAY) as month, 
             (select count(*) from activity_log al where activity_type = 7 and al.user_id = u.id and createdate >= DATE(NOW()) - INTERVAL 7 DAY) as week
           from user u """).as(User.stat. *)
    }
  }

  def update(user: UserFull, email: String, settings: String) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update user set email = {email}, username = {username}, fullname = {fullname}, 
         settings = {settings}, phone = {phone}, city = {city}
         where id = {id} 
        """).on(
          'email -> email,
          'username -> user.username,
          'fullname -> user.fullname,
          'settings -> settings,
          'phone -> user.phone,
          'city -> user.city,
          'id -> user.id).executeUpdate
    }
  }

  def updatelastlogindate(email: String) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         update user set lastlogindate = {lastlogindate} where 
         email = {email} 
        """).on(
          'email -> email,
          'lastlogindate -> new Date()).executeUpdate
    }
  }

  def updatepassword(email: String, password: String) = {
    DB.withConnection { implicit connection =>
      SQL(
        "update user set password = {password} where email = {email} ").on(
          'email -> email,
          'password -> hash(password)).executeUpdate
    }
  }

  def delete(id: Long) = {
    DB.withConnection { implicit connection =>
      SQL("delete from user where id = {id} ").on('id -> id).executeUpdate
    }
  }
  
  def authenticate(email: String, password: String): Option[User] = {
    Logger.info("try to authenticate user: " + email)
    DB.withConnection { implicit connection =>
      SQL(
        """
         select id, email, username, password from user where 
         email = {email} and password = {password}
        """).on(
          'email -> email,
          'password -> hash(password)).as(User.simple.singleOpt)
    }
  }

  def create(user: UserFull): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into user (email, username, password, createdate, deleted, settings, type, openidtoken, fullname, city, country, state, phone) 
          values ( {email}, {username}, {password}, {createdate}, {deleted}, {settings}, {type}, {openidtoken}, {fullname}, {city}, {country}, {state}, {phone} )
        """).on(
          'email -> user.email,
          'username -> user.username,
          'password -> hash(user.password),
          'createdate -> new Date(),
          'deleted -> user.deleted,
          'settings -> user.settings,
          'type -> user.ttype,
          'openidtoken -> user.openidtoken,
          'fullname -> user.fullname,
          'city -> user.city,
          'country -> user.country,
          'state -> user.state,
          'phone -> user.phone).executeInsert()
    }
  }

  def create(email: String, password: String, password2: String) = {
    Logger.info ("creating user with email:" + email)
    
    if (password != null && password.equals(password2) && getFullUser(email).isEmpty) {
      DB.withConnection { implicit connection =>
      SQL(
        """
          insert into user (email, username, password, createdate) values (
            {email}, {username}, {password}, {createdate}
          )
        """).on(
          'email -> email,
          'username -> email,
          'password -> hash(password),
          'createdate -> new Date()).executeInsert()
      }
    } else {
      Logger.info("do *not* create new user " + email)
      None
    }
  }

  def countAll(): Long = {
    DB.withConnection { implicit connection =>
      SQL("select count(*) from user ").as(scalar[Long].single)
    }
  }
  
  def hash(str: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    val ha = new sun.misc.BASE64Encoder().encode(md.digest((str + "x5a.63uwx").getBytes))
    ha.toString()
  }
}

object UserProfile {
  implicit val userProfileReads = Json.reads[UserProfile]
  implicit val userProfileWrites = Json.writes[UserProfile]
}

object UserFull {
  implicit val userReads = Json.reads[UserFull]
  implicit val userWrites = Json.writes[UserFull]
}

object UserStats {
  implicit val userStatsReads = Json.reads[UserStat]
  implicit val userStatsWrites = Json.writes[UserStat]
  }
