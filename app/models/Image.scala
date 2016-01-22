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


case class Image(id: Long, filename: String, url: String, restaurant_id: Long, dish_id: Long, status: Int, lastupdate: Date)

object Image {

  val blankImage = new Image(0, null, null, 0, 0, 0, null)
    
  val simple = {
    get[Long]("image.id") ~
      get[String]("image.filename") ~
      get[String]("image.url") ~
      get[Long]("image.restaurant_id") ~
      get[Long]("image.dish_id") ~
      get[Int]("image.status") ~
      get[Date]("image.lastupdate") map {
        case id ~ filename ~ url ~ restaurant_id ~ dish_id ~ status ~lastupdate => Image(id, filename, url, restaurant_id, dish_id, status, lastupdate)
      }
  }

  def findAll(owner: Long): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, status, lastupdate from image ").on().as(Image.simple *)
    }
  }

  def findByRestaurant(id: Long): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, status, lastupdate from image where status >= 0 and restaurant_id = {restaurant_id} order by id desc ")
         .on('restaurant_id -> id).as(Image.simple *)
    }
  }

  def findByDish(id: Long): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, status, lastupdate from image where status >= 0 and dish_id = {dish_id} order by id desc ")
         .on('dish_id -> id).as(Image.simple *)
    }
  }
  
  def create(image: Image): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into image (filename, url, restaurant_id, dish_id, status, lastupdate) values (
          {filename}, {url}, {restaurant_id}, {dish_id}, {status}, {lastupdate}
          )
        """).on(
          'filename -> image.filename,
          'url -> image.url,
          'restaurant_id -> image.restaurant_id,
          'dish_id -> image.dish_id,
          'status -> image.status,
          'lastupdate -> new Date()).executeInsert()
    }
  }

  def createUrl(str: String): String = {
    val url = current.configuration.getString("image.server.baseurl")
    Logger.info("Image using base url " + url)
    url.getOrElse("http://localhost") + str  
  }
  
  def updateRestaurantImages(restaurantId: Long, status: Int) = {
    DB.withConnection { implicit connection =>
      SQL("update image set status = {status} where restaurant_id = {restaurant_id}").on(
          'restaurant_id -> restaurantId,
          'lastupdate -> new Date(),
          'status -> status).executeUpdate
    }
  }
  
  def updateDishImages(dishId: Long, status: Int) = {
    DB.withConnection { implicit connection =>
      SQL("update image set status = {status} where dish_id = {dish_id}").on(
          'dish_id -> dishId,
          'lastupdate -> new Date(),
          'status -> status).executeUpdate
    }
  }
  
  implicit val imageReads = Json.reads[Image]
  implicit val imageWrites = Json.writes[Image]

}

