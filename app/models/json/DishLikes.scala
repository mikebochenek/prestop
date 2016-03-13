package models.json

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.mutable.MutableList

case class DishLikesContainer(likes: MutableList[DishLikes])

case class DishLikes(user_id: Long, dish_id: Long, id: Long, price: String, name: String, liked: Boolean, 
    greenScore: Double, greenScoreTags: Seq[String], url: String, url_large: String, distance: String, ingredients: Seq[String],
    restaurantID: Long, var restaurantName: String, var restaurantUrl: String, 
    var friendLikeUrls: Seq[String],
    diet: Seq[String], dishType: Seq[String], meatOrigin: Seq[String])

object DishLikes {
  implicit val dishLikesReads = Json.reads[DishLikes]
  implicit val dishLikesWrites = Json.writes[DishLikes]
}