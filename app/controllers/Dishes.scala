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
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.iteratee._
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files
import scala.collection.mutable.MutableList
import common.RecommendationUtils
import scala.util.control.Exception.allCatch
import models.json.BasicDish
import models.json.DistanceDetailDish
import common.Haversine

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
        case "restaurant" => Image.findByRestaurant(id).filter { x => x.status == 0 }
          .sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        case "restaurantProfile" => Image.findByRestaurant(id).filter { x => x.status == 1 }
          .sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
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
      Logger.info("cropping - calling dish crop POST - type: " + imgType + " id:" + id 
          + "cropping x:" + x + " y:" + y + " w:" + w + " h:" + h)
      
      //1242 ratio! don't forget ratio could be greater than one, and less than one
      val originalImg = imgType match {
        case "dish" => Image.findByDish(id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        case "restaurant" => Image.findByRestaurant(id).filter { x => x.status == 0 }
          .sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        case "restaurantProfile" => Image.findByRestaurant(id).filter { x => x.status == 1 }
          .sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
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
      val user = User.getFullUser(username).get
      val dish = "7".equals(user.ttype) match {
        case true => Dish.findById(username, id)
        case false => {
          val _dish = Dish.findById(username, id)
          if (Restaurant.findAllByUser(user.id).exists { x => x.id == _dish(0).restaurant_id }) {
            _dish
          } else {
            null
          }
        }
      } 
      
      val tags = Tag.findByRef(id, 11).map(_.name)
      val greenscoretags = Tag.findByRef(id, Tag.TYPE_GREENSCORE).map(_.name) //here we must return name (not the english/german text)
      val diet = Tag.findByRef(id, 34).map(_.name)
      val dishtype = Tag.findByRef(id, 35).map(_.name)
      val meatorigin = Tag.findByRef(id, 36).map(_.name)
      val url = Image.findByDish(id).sortBy{ _.id }.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      val restaurantName = Restaurant.findById("", dish(0).restaurant_id)(0).name
      val uploadUser = getUploadUser(dish(0).id)

      //val allTags = tags ++ diet ++ dishtype ++ greenscoretags
      val searchTags = Tag.findByRef(id, Tag.TYPE_SEARCH).map(_.name)
      
      dish.foreach { x => x.greenScore = calculateGreenScore(greenscoretags.size) }
      
      Ok(views.html.dish_edit(dishForm, dish(0), url, tags.mkString(", "), greenscoretags.mkString(", "), 
          diet.mkString(", "), dishtype.mkString(", "), meatorigin.mkString(", "), restaurantName, searchTags.sorted.mkString(", "), uploadUser))
    }
  }
  
  def getUploadUser(id: Long) = {
    val activityLogUpload = ActivityLog.findAllBySubType(ActivityLog.TYPE_DISH_UPLOAD, id)
    activityLogUpload.size match {
        case 1 => { User.getFullUser(activityLogUpload(0).user_id)}
        case _ => null
    }    
  }
  
  def calculateGreenScore(size: Int) = {
    val totalPossibleGreenscoreTags = Tag.findAll().filter { x => x.status == Tag.TYPE_GREENSCORE }.size 
    size * 100 / totalPossibleGreenscoreTags 
  }

  
  val dishForm = Form(
    tuple(
      "id" -> text,
      "searchtags" -> text,
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
      val (id, searchtags, price, name, serving, description, greenscore, restaurant_id, status, itags, greenscoretags, 
          diet, dishtype, meatorigin, source) = dishForm.bindFromRequest.get
      var validationErrors = ""      
      val fullUser = User.getFullUser(username).get
      val newStatus = status.toInt /*("7".equals(fullUser.ttype) || status.toInt == -1) match {
        case true => status.toInt
        case false => 4
      }*/
      Logger.debug("Dish.save for id: " + id + " restaurant_id: " + restaurant_id + " by: " + fullUser + " searchtags: " + searchtags)    
      
      if (!(allCatch opt price.toDouble).isDefined) {
        validationErrors += "Price should be a valid number (i.e. 15.90)"
      }
      
      if (validationErrors.length() == 0) {
        Dish.update(id.toLong, price.toDouble, name, greenscore.toDouble, newStatus, serving, description, source, restaurant_id.toLong)
        Tag.updateTags(id.toLong, itags, 11)
        Tag.updateTags(id.toLong, greenscoretags, 31)
        Tag.updateTags(id.toLong, diet, 34)
        Tag.updateTags(id.toLong, dishtype, 35)
        Tag.updateTags(id.toLong, meatorigin, 36)
        Tag.updateTags(id.toLong, searchtags, Tag.TYPE_SEARCH)
        //Tag.updateTags(id.toLong, tags, Seq(11, 31, 34, 35)) //NB: order is important, 11 should be first, because we will create with status=11
        Logger.info("calling dish update for id:" + id + " price:" + price + " newStatus:" + newStatus 
            + " name:" + name + " itags:" + itags + " greenscoretags: " + greenscoretags + " searchtags: " + searchtags)
        Redirect(routes.Dishes.getById(id.toLong)).flashing("success" -> ("Changes saved successfully at " 
            + RecommendationUtils.currentTime()))
      } else {
        Logger.info("failed calling dish update for id:" + id + " price:" + price + " newStatus:" + newStatus 
            + " name:" + name + " tags:" + itags + " greenscoretags: " + greenscoretags + " validationErrors: " + validationErrors)
        Redirect(routes.Dishes.getById(id.toLong)).flashing("error" -> validationErrors)
      }
  }

  
  def createRI(dish: Dish, r: Restaurant, friendLikedDishURLs: Seq[String], greenscoretags: Seq[String], like: Boolean) = {
    new RecommendationItem(dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, 
       dish.source, dish.description.getOrElse(""), like, calculateGreenScore(greenscoretags.size), 
       greenscoretags,
       Image.findByDish(dish.id).filter{x => x.width.get == 172}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
       Image.findByDish(dish.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
       null,
       Tag.findByRef(dish.id, 11).map(_.name),
       r.id,
       r.name, r.city + ", " + r.misc.country.getOrElse(""), 
       Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
       friendLikedDishURLs,
       Tag.findByRef(dish.id, 34).map(_.name),
       Tag.findByRef(dish.id, 35).map(_.name),
       Tag.findByRef(dish.id, 36).map(_.name), 0)
  }
  
  def getActiveDishes(longitude: String, latitude: String, maxDishes: Long) = Action {
    implicit request => {  
      val dishes = Dish.findAll().take(maxDishes.toInt)
      val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)
      val result = new Recommendations(MutableList.empty);

      for (dish <- dishes) {
        dish.name = dish.name.replaceAll("ü", "u").replaceAll("ä", "a").replaceAll(" ", "_")
           .replaceAll("\\W+", "").replaceAll("_", "-").toLowerCase
        val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse(""))
      
        if (restaurants.get(dish.restaurant_id).isEmpty) {
          Logger.debug("deleted restaurant - restaurants.get(dish.restaurant_id) " + dish.restaurant_id + " dish_id:" + dish.id)
        } else {
          val r = restaurants.get(dish.restaurant_id).head
          result.dishes += createRI(dish, r, Seq.empty, greenscoretags, false)
        }
      }
      
      Ok(Json.prettyPrint(Json.toJson(result.dishes.map(a => Json.toJson(a)))))
    }
  }
  
  def getDishDetails(user_id: Long, dish_id: Long, longitude: String, latitude: String) = Action {
    implicit request => {  
      val dishes = Dish.findById(null, dish_id)
      val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)
      val result = new Recommendations(MutableList.empty);
      val allLikes = ActivityLog.findAllByUserType(user_id, 11)
      val dishLikers = Friend.findDishLikers((dishes.map { x => x.id }).toList, user_id)  

      for (dish <- dishes) {
        val like = !(allLikes.find { x => x.activity_subtype == dish.id }.isEmpty)
        val friendLikedDishURLs = dishLikers.filter { x => x.dish_id == dish.id && x.friend_image_url != null}
          .map { y => y.friend_image_url }  //TODO in cases where its null, should we show a default image?
        val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse(""))
        
        if (restaurants.get(dish.restaurant_id).isEmpty) {
          Logger.debug("deleted restaurant - restaurants.get(dish.restaurant_id) " + dish.restaurant_id + " dish_id:" + dish.id)
        } else {
          val r = restaurants.get(dish.restaurant_id).head
          result.dishes += createRI(dish, r, friendLikedDishURLs, greenscoretags, like)
        }
      }
      
      Ok(Json.prettyPrint(Json.toJson(result.dishes.map(a => Json.toJson(a)))))
    }
  }
  
  def getAllForUser(restId: Long, userId: Long) = Action {
    implicit request => {
      val dishes = restId match {
        case -1 => Dish.findAll()
        case _  => Dish.findAll(restId)
      }

      //TODO all the below is way too similar to Recommendation.recommend and should be refactored into a common method
      val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)

      val allLikes = ActivityLog.findAllByUserType(userId, 11)
      val dishLikers = Friend.findDishLikers((dishes.map { x => x.id }).toList, userId)  
      val result = new Recommendations(MutableList.empty);
    
      for (dish <- dishes) {
        val like = !(allLikes.find { x => x.activity_subtype == dish.id }.isEmpty)
      
        val friendLikedDishURLs = dishLikers.filter { x => x.dish_id == dish.id && x.friend_image_url != null}
          .map { y => y.friend_image_url }  //TODO in cases where its null, should we show a default image?
              
        val greenscoretags = Tag.findByRef(dish.id, Tag.TYPE_GREENSCORE).map(_.en_text.getOrElse(""))

      
        val r = restaurants.get(dish.restaurant_id).head
        val ri = new RecommendationItem(dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, 
            dish.source, dish.description.getOrElse(""), like, calculateGreenScore(greenscoretags.size), 
          greenscoretags,
          Image.findByDish(dish.id).filter{x => x.width.get == 172}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          Image.findByDish(dish.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          null,
          Tag.findByRef(dish.id, 11).map(_.name),
          r.id,
          r.name, r.city + ", " + r.misc.country.getOrElse(""), 
          Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
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
        val ri = new RecommendationItem(dish.id, RecommendationUtils.makePriceString(dish.price), dish.name, 
            dish.source, dish.description.getOrElse(""), like, calculateGreenScore(greenscoretags.size), 
          greenscoretags,
          Image.findByDish(dish.id).filter{x => x.width.get == 172}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          Image.findByDish(dish.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url,
          null,
          Tag.findByRef(dish.id, 11).map(_.name),
          r.id,
          r.name, r.city + ", " + r.misc.country.getOrElse(""), 
          Image.findByRestaurant(r.id).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
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
      val id = Dish.create(restId.as[String].toLong, 0.0, null, txt.as[String], 0.0, 0);
      Logger.info("dish created with " + txt.as[String] + " with id:" + id + " restaurantID:"+ restId.as[String].toLong)
      Ok("ok")
    }
  }

  /**
   * Called from ImageGrabber with a particular input..
   */
  def createAndPopulate(name: String, tags: String, restaurant: Long, jsonFilename: String, 
      imageFilename: String) = IsAuthenticated { username =>
    implicit request => {
      
      Logger.debug("Dish.createAndPopulate name: " + name + " tags: " + tags 
          + " restaurant: " + restaurant + " jsonFilename: " + jsonFilename + " imageFilename: " + imageFilename)
      
      val id = Dish.create(restaurant, 0.0, null, name, 0.0, 4).get;
      val restaurantName = "todo restaurantName" //TODO restaurantName - how do we get it from json, or do we allow user to change it later?
      val url = ImageGrabber.createUrl(imageFilename, jsonFilename) 
      val dish = Dish.findById("", id)
      
      //TODO still need to save tags (or worst case, wait for next "save" action
      
      val filename = ImageGrabber.getPath + jsonFilename.dropRight(5) + url.substring(url.lastIndexOf('/'))
      Logger.debug("filename: " + filename)
      Image.saveAndResizeImages(FilePart("qqfile", "dish" + id + ".jpg", Some("image/jpeg"), TemporaryFile(new File(filename))), id, "dish") 

      Ok(views.html.dish_edit(dishForm, dish(0), url, tags, "" /* greenscore */, 
          "" /* diet */, "" /* dishtype */, "" /* meatorigin */, restaurantName, tags, null))
    }
  }
  
  def upload(id: Long) = Action(parse.multipartFormData) { request =>
    Logger.info("request.Content-Type   : " + request.headers.get("Content-Type"))
    Logger.info("request.Accept-Charset : " + request.headers.get("Accept-Charset"))
    request.body.file("picture").map { picture =>
      Image.saveAndResizeImages(picture, id, "dish")
      Redirect(routes.Dishes.cropImage(id, "dish"))
    }.getOrElse {
      Redirect(routes.Dishes.getById(id)).flashing(
        "error" -> "Missing file")
    }
  }  

  // http://stackoverflow.com/questions/20322528/uploading-images-to-server-android
  def uploadDish(user_id: Long, dish_name: String, price: String, price_bucket: String, place_id: String) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Logger.info("uploadDish userId:" + user_id + " name: " + dish_name + " price: " + price 
          + " price_bucket: " + price_bucket + " place_id: " + place_id)

      try {
        val rest = Restaurant.findAll().filter { x => place_id.equals(x.misc.place_id.getOrElse("")) }
        val restId = (rest.isEmpty) match  {
          case true => Restaurant.create(place_id, "", "", 0.0, 0.0, "", 0, None, 4).get
          case false => rest(0).id
        }
      
        if (rest.isEmpty) { //hmmm.. kinda hacky, but we need to store place_id
          val r = Restaurant.findById("", restId)(0)
          Restaurant.update(r.id, r.name, r.city, r.address, r.longitude, r.latitude, r.schedule, r.restype, 
            r.status, "", "", "", "", "", "", place_id)
        }
      
        val id = Dish.create(restId, extractPrice(price), (price_bucket match { case "" => null; case _ => price_bucket}), dish_name, 0.0, 4);
        Logger.info("dish created with " + dish_name + " with id:" + id + " restaurantID:"+ restId + " existing? " + !rest.isEmpty)
        Image.saveAndResizeImages(picture, id.get, "dish")
        
        ActivityLog.create(user_id, ActivityLog.TYPE_DISH_UPLOAD, id.get, "")
      
        Ok(Json.prettyPrint(Json.toJson(CommonJSONResponse.OK)))
      } catch {
        case e: javax.imageio.IIOException => {
          Logger.error("uploadDish failure ", e)
          Ok(Json.prettyPrint(Json.toJson(ErrorJSONResponse("error", e.toString))))
        }
      } 
    }.getOrElse {
      Ok(Json.prettyPrint(Json.toJson(ErrorJSONResponse("error", "missing file - http post required with param named 'picture'"))))
    }
  }  
  
  /** this allows to upload dishes without a price, we assign zero by default */
  def extractPrice(p: String) = {
    try {
      p.replaceAll("[A-Za-z]", "").trim.toDouble
    } catch {
      case _ : Throwable => { 0 }
    }
  }
  
  def getBarChartData(id: Long): String = {
    val dishes = Dish.findById("", id)
    Restaurants.getBarChartData(dishes)
  }
  
  def getAllBasic() = Action {
    implicit request => {
      val dishes = Dish.findAll().filter { x => x.status == 0 } // only active
      val basicDishes = dishes.map { dish => BasicDish(dish.id, dish.name, Restaurant.findById("", dish.restaurant_id)(0).name) }
      Ok(Json.prettyPrint(Json.toJson(basicDishes.sortBy { _.name })))
    }
  }
  
  def getByID(id: Long, longitude: String, latitude: String) = Action {
    implicit request => {
      var json = ""
      val dish = Dish.findById("", id)
      if (dish.size > 0) {
        val ingredients = Tag.findByRef(dish(0).id, 11).map(_.name).mkString(",")
        val r = Restaurant.findById("", dish(0).restaurant_id)(0)
        val dist = Haversine.haversine(r.latitude, r.longitude, 
          Recommend.parseLatitude(longitude), Recommend.parseLongitude(latitude))
        val distance = RecommendationUtils.makeCityDistanceString(dist, r.city)

        val details = DistanceDetailDish(dish(0).id, dish(0).name, dish(0).description.getOrElse(""), 
            dish(0).price, ingredients, distance, dist * 1000, r.longitude, r.latitude, 
            Image.findByDish(dish(0).id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url.replace("http:", "https:"),
            r.name, r.id, r.city)
        json = Json.prettyPrint(Json.toJson(details))
      } else {
        json = Json.prettyPrint(Json.toJson(ErrorJSONResponse("dish not found", "" + id)))
      }
      Ok(json)
    }
  }

}
