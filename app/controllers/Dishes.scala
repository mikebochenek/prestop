package controllers

import java.io.File
import play.Play
import play.api.mvc.Action
import play.api.mvc.Session
import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import models._
import views._
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import play.api.libs.Files
import scala.collection.mutable.MutableList
import common.RecommendationUtils

object Dishes extends Controller with Secured {

  val dishCropForm = Form(
    tuple(
      "x" -> text,
      "y" -> text,
      "w" -> text,
      "h" -> text))

  val maxW = 700.0
  def cropImage(id: Long, imgType: String) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling dish crop - load data for id:" + id)
      val img = imgType match {
        case "dish" => Image.findByDish(id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        case "restaurant" => Image.findByRestaurant(id).filter { x => x.status == 0 }.sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        case "restaurantProfile" => Image.findByRestaurant(id).filter { x => x.status == 1 }.sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
      }
        
      Ok(views.html.dish_crop(imgType, img.url, id, img.width.get, img.height.get, img.width.get / maxW))
    }
  }
  
  def adjust(w: Int, x: Int) = {
    (w / maxW * x).toInt
  }

  def cropImagePost(id: Long, imgType: String) = IsAuthenticated { username =>
    implicit request => {
      val (x, y, w, h) = dishCropForm.bindFromRequest.get
      Logger.info("cropping - calling dish crop POST - type: " + imgType + " id:" + id + "cropping x:" + x + " y:" + y + " w:" + w + " h:" + h)
      
      //1242 ratio! don't forget ratio could be greater than one, and less than one
      val originalImg = imgType match {
        case "dish" => Image.findByDish(id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        case "restaurant" => Image.findByRestaurant(id).filter { x => x.status == 0 }.sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        case "restaurantProfile" => Image.findByRestaurant(id).filter { x => x.status == 1 }.sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
      }
      val wd = originalImg.width.get.toInt
      Image.crop(originalImg.id, adjust(wd, x.toInt), adjust(wd, y.toInt), adjust(wd, w.toInt), adjust(wd, h.toInt))
      
      if ("dish".equals(imgType)) {
        Redirect(routes.Dishes.getById(id))
      } else {
        Redirect(routes.Restaurants.edit(id))
      }
    }
  }
  
  def getById(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling dish edit - load data for id:" + id)
      val dish = Dish.findById(username, id)
      val tags = Tag.findByRef(id, 11).map(_.name).mkString(", ")
      val greenscoretags = Tag.findByRef(id, Tag.TYPE_GREENSCORE).map(_.name) //here we must return name (not the english/german text)
      val diet = Tag.findByRef(id, 34).map(_.name).mkString(", ")
      val dishtype = Tag.findByRef(id, 35).map(_.name).mkString(", ")
      val meatorigin = Tag.findByRef(id, 36).map(_.name).mkString(", ")
      val url = Image.findByDish(id).sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url

      dish.foreach { x => x.greenScore = calculateGreenScore(greenscoretags.size) }
      
      Ok(views.html.dish_edit(dishForm, dish(0), url, tags, greenscoretags.mkString(", "), diet, dishtype, meatorigin))
    }
  }
  
  def calculateGreenScore(size: Int) = {
    val totalPossibleGreenscoreTags = Tag.findAll().filter { x => x.status == Tag.TYPE_GREENSCORE }.size 
    size * 100 / totalPossibleGreenscoreTags 
  }

  
  val dishForm = Form(
    tuple(
      "id" -> text,
      "price" -> text,
      "name" -> text,
      "serving" -> text,
      "description" -> text,
      "greenscore" -> text,
      "restaurant_id" -> text,
      "status" -> text,
      "itags" -> text,
      "greenscoretags" -> text,
      "diet" -> text,
      "dishtype" -> text,
      "meatorigin" -> text,
      "source" -> text))
      
  def save = IsAuthenticated { username =>
    implicit request =>  
      val (id, price, name, serving, description, greenscore, restaurant_id, status, itags, greenscoretags, diet, dishtype, meatorigin, source) = dishForm.bindFromRequest.get
      
      val fullUser = User.getFullUser(username).get
      val newStatus = ("7".equals(fullUser.ttype) || status.toInt == -1) match {
        case true => status.toInt
        case false => 4
      }
      
      Dish.update(id.toLong, price.toDouble, name, greenscore.toDouble, newStatus, serving, description, source)
      Tag.updateTags(id.toLong, itags, 11)
      Tag.updateTags(id.toLong, greenscoretags, 31)
      Tag.updateTags(id.toLong, diet, 34)
      Tag.updateTags(id.toLong, dishtype, 35)
      Tag.updateTags(id.toLong, meatorigin, 36)
      Logger.info("calling restaurant update for id:" + id + " price:" + price + " newStatus:" + newStatus + " name:" + name + " tags:" + itags + " greenscoretags: " + greenscoretags)
      Redirect(routes.Dishes.getById(id.toLong))
  }

  def getAllForUser(restId: Long, userId: Long) = Action {
    implicit request => {
      val dishes = Dish.findAll(restId)

      //TODO all the below is way too similar to Recommendation.recommend and should be refactored into a common method
      val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)

      val allLikes = ActivityLog.findAllByUserType(userId, 11)
      val dishLikers = Friend.findDishLikers((dishes.map { x => x.id }).toList, userId)  
      val result = new Recommendations(MutableList.empty);
    
      for (dish <- dishes) {
        val like = !(allLikes.find { x => x.activity_subtype == dish.id }.isEmpty)
      
        val friendLikedDishURLs = dishLikers.filter { x => x.dish_id == dish.id && x.friend_image_url != null}.map { y => y.friend_image_url }  //TODO in cases where its null, should we show a default image?
              
        val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse(""))

      
        val r = restaurants.get(dish.restaurant_id).head
        val ri = new RecommendationItem(dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, dish.source, dish.description.getOrElse(""), like, calculateGreenScore(greenscoretags.size), 
          greenscoretags,
          Image.findByDish(dish.id).filter{x => x.width.get == 172}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          Image.findByDish(dish.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          null,
          Tag.findByRef(dish.id, 11).map(_.name),
          r.id,
          r.name, r.city + ", " + r.misc.country.getOrElse(""), Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
          friendLikedDishURLs,
          Tag.findByRef(dish.id, 34).map(_.name),
          Tag.findByRef(dish.id, 35).map(_.name),
          Tag.findByRef(dish.id, 36).map(_.name), 0)
        result.dishes += ri
      }
      
      Ok(Json.prettyPrint(Json.toJson(result.dishes.map(a => Json.toJson(a)))))
    }
  } 
  
  def getAll(restId: Long) = Action {
    implicit request => {
      val dishes = Dish.findAll(restId)

      //TODO all the below is way too similar to Recommendation.recommend and should be refactored into a common method
      val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)

      val result = new Recommendations(MutableList.empty);
    
      for (dish <- dishes) {
        val allLikes = Activities.getLikeActivitiesByDish(dish.id)
        val like = false //TODO? !(allLikes.find { x => x.id == user.id }.isEmpty)
      
        val friendLikedDishURLs = allLikes.map(x => x.profileImageURL)
        //Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url  :: Nil
        
        val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse(""))

      
        val r = restaurants.get(dish.restaurant_id).head
        val ri = new RecommendationItem(dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, dish.source, dish.description.getOrElse(""), like, calculateGreenScore(greenscoretags.size), 
          greenscoretags,
          Image.findByDish(dish.id).filter{x => x.width.get == 172}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          Image.findByDish(dish.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          null,
          Tag.findByRef(dish.id, 11).map(_.name),
          r.id,
          r.name, r.city + ", " + r.misc.country.getOrElse(""), Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
          friendLikedDishURLs,
          Tag.findByRef(dish.id, 34).map(_.name),
          Tag.findByRef(dish.id, 35).map(_.name),
          Tag.findByRef(dish.id, 36).map(_.name), 0)
        result.dishes += ri
      }
      
      Ok(Json.prettyPrint(Json.toJson(result.dishes.map(a => Json.toJson(a)))))
    }
  } 
  
  def getTags(ttype: Long) = Action {
    implicit request => {
      val all = ttype match {
        case 0 => Tag.findAll()
        case _ => Tag.findAll().filter { x => x.status == ttype }
      }
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  }

  def create() = IsAuthenticated { username =>
    implicit request => {
      val txt = (request.body.asJson.get \ "donetext")
      val restId = (request.body.asJson.get \ "restaurantID")
      val id = Dish.create(restId.as[String].toLong, 0.0, txt.as[String], 0.0, 0);
      Logger.info("dish created with " + txt.as[String] + " with id:" + id + " restaurantID:"+ restId.as[String].toLong)
      Ok("ok")
    }
  }

  def upload(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Image.saveAndResizeImages(picture, id, "dish")
      Redirect(routes.Dishes.cropImage(id, "dish"))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
  
  // http://stackoverflow.com/questions/20322528/uploading-images-to-server-android
  def uploadDish(userId: Long, name: String, price: String) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Logger.info("uploadDish userId:" + userId + " name: " + name + " price: " + price)
      //TODO Image.saveAndResizeImages(picture, id, "dish")
      Redirect(routes.Dishes.cropImage(1, "dish"))
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  }  
  
}
