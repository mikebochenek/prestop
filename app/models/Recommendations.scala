package models

import play.api.db._
import play.api.Play.current
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Logger
import anorm._
import anorm.SqlParser._
import java.util.Date
import scala.collection.mutable.MutableList

case class Recommendations(dishes: MutableList[RecommendationItem])

case class RecommendationItem(id: Long, price: String, name: String, dishSource: String, dishDescription: String, liked: Boolean, 
    var greenScore: Double, var greenScoreTags: Seq[String], var url: String, var url_large: String, 
    var distance: String, var ingredients: Seq[String],
    restaurantID: Long, var restaurantName: String, restaurantLocation: String, var restaurantUrl: String,  
    var friendLikeUrls: Seq[String],
    var diet: Seq[String], var dishType: Seq[String], var meatOrigin: Seq[String], var score: Double)

object RecommendationItem {
  implicit val recommendationItemsReads = Json.reads[RecommendationItem]
  implicit val recommendationItemsWrites = Json.writes[RecommendationItem]
}

object Recommendations {

  def getLikedDishes(id: Long): Seq[LikedDishes] = {
    DB.withConnection { implicit connection =>
      SQL("""
        select a.createdate, d.id, d.name, t.id, t.name from user u 
        join activity_log a on a.user_id = u.id 
        join dish d on d.id = a.activity_subtype and d.status >= 0
        join tagref tr on tr.refid = d.id and tr.status = 11
        join tag t on t.id = tr.tagid
        where a.activity_type = 11 and u.id = {id}
        """).on(
        'id -> id).as(LikedDishes.all *)
    }
  }
  
  implicit val recommendationsReads = Json.reads[Recommendations]
  implicit val recommendationsWrites = Json.writes[Recommendations]
}

case class LikedDishes(createDate: Date, dishId: Long, dishName: String, tagId: Long, tagName: String, friendId: Long)

object LikedDishes {
  val all = {
    get[Date]("activity_log.createdate") ~
    get[Long]("dish.id") ~
    get[String]("dish.name") ~
    get[Long]("tag.id") ~
    get[String]("tag.name") map {
      case createDate ~ dishId ~ dishName ~ tagId ~ tagName => LikedDishes(createDate, dishId, dishName, tagId, tagName, 0)
    }
  }
  
  implicit val dishReads = Json.reads[LikedDishes]
  implicit val dishWrites = Json.writes[LikedDishes]
}