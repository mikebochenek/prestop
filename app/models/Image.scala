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
import play.api.mvc._
import play.api.libs._
import play.api.data._
import java.io.File
import javax.imageio.ImageIO
import org.imgscalr.Scalr


case class Image(id: Long, filename: String, url: String, restaurant_id: Long, dish_id: Long, 
    user_id: Option[Long], width: Option[Long], height: Option[Long], status: Int, lastupdate: Date)

object Image {

  val blankImage = new Image(0, null, null, 0, 0, null, null, null, 0, null)
    
  val simple = {
    get[Long]("image.id") ~
      get[String]("image.filename") ~
      get[String]("image.url") ~
      get[Long]("image.restaurant_id") ~
      get[Long]("image.dish_id") ~
      get[Option[Long]]("image.user_id") ~
      get[Option[Long]]("image.width") ~
      get[Option[Long]]("image.height") ~
      get[Int]("image.status") ~
      get[Date]("image.lastupdate") map {
        case id ~ filename ~ url ~ restaurant_id ~ dish_id ~ user_id ~ width ~ height ~ status ~lastupdate => Image(id, filename, url, restaurant_id, dish_id, user_id, width, height, status, lastupdate)
      }
  }

  def findAll(): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, user_id, width, height, status, lastupdate from image ").on().as(Image.simple *)
    }
  }

  def findByRestaurant(id: Long): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, user_id, width, height, status, lastupdate from image where status >= 0 and restaurant_id = {restaurant_id} order by id asc ")
         .on('restaurant_id -> id).as(Image.simple *)
    }
  }

  def findById(id: Long): Image = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, user_id, width, height, status, lastupdate from image where status >= 0 and id = {id} order by width desc ")
         .on('id -> id).as(Image.simple.single)
    }
  }
  
  def findByDish(id: Long): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, user_id, width, height, status, lastupdate from image where status >= 0 and dish_id = {dish_id} order by id asc ")
         .on('dish_id -> id).as(Image.simple *)
    }
  }

  def findByUser(id: Long): Seq[Image] = {
    DB.withConnection { implicit connection =>
      SQL("select id, filename, url, restaurant_id, dish_id, user_id, width, height, status, lastupdate from image where status >= 0 and user_id = {user_id} order by width desc ")
         .on('user_id -> id).as(Image.simple *)
    }
  }
  
  def create(image: Image): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into image (filename, url, restaurant_id, dish_id, user_id, width, height, status, lastupdate) values (
          {filename}, {url}, {restaurant_id}, {dish_id}, {user_id}, {width}, {height}, {status}, {lastupdate}
          )
        """).on(
          'filename -> image.filename,
          'url -> image.url,
          'restaurant_id -> image.restaurant_id,
          'dish_id -> image.dish_id,
          'user_id -> image.user_id,
          'width -> image.width,
          'height -> image.height,
          'status -> image.status,
          'lastupdate -> new Date()).executeInsert()
    }
  }

  def createUrl(str: String): String = {
    val url = current.configuration.getString("image.server.baseurl")
    //Logger.info("Image using base url " + url)
    url.getOrElse("http://localhost") + str  
  }
  
  def getImageLocalPath(): String = {
    val url = current.configuration.getString("image.server.localpath")
    url.getOrElse("/home/mike/data/presto/")
  }
  
  def updateRestaurantImages(restaurantId: Long, previousStatus: Int, status: Int) = {
    DB.withConnection { implicit connection =>
      SQL("update image set status = {status} where restaurant_id = {restaurant_id} and status = {prevStatus}").on(
          'restaurant_id -> restaurantId,
          'lastupdate -> new Date(),
          'prevStatus -> previousStatus,
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

  val resolutions = List(48, 72, 172, 640, 750, 1242)
  
  def reverseURLGen(url: String, w: Int) = {
    val urlParts = url.split("/")
    val filename = urlParts(urlParts.length - 1)
    var newURL = ""
    for((x,i) <- urlParts.view.zipWithIndex) {
      if (i < urlParts.length - 1) {
        newURL += x + "/"
      }
    }
    newURL += filename.replace(".", ("-" + w + "."))
    newURL
  }
  
  def saveAndResizeImages(picture: MultipartFormData.FilePart[Files.TemporaryFile], id: Long, kind: String) {
    val filename = picture.filename
    val ts = System.currentTimeMillis()
    
    var restID = 0L; var dishID = 0L; var userID = 0L; var status = 0;
    
    if ("dish".equals(kind)) {
      Image.updateDishImages(id, -1)
      dishID = id
    }
      
    if ("restaurant".equals(kind)) {
      Image.updateRestaurantImages(id, 0, -1)
      restID = id
    }

    if ("restaurantlogo".equals(kind)) {
      Image.updateRestaurantImages(id, 1, -1)
      restID = id
      status = 1
    }

    if ("user".equals(kind)) {
      userID = id
    }
    
    val path = getImageLocalPath() + ts
    val file = new File(path + s"/$filename")
    val extension = filename.takeRight(3).toLowerCase
    
    picture.ref.moveTo(file)
    val img = ImageIO.read(file); // load image
    Image.create(new Image(0, file.getAbsolutePath, Image.createUrl(ts + "/" + file.getName), restID, dishID, Some(userID), Some(img.getWidth), Some(img.getHeight.toLong), status, null))
      
    //http://www.htmlgoodies.com/beyond/java/create-high-quality-thumbnails-using-the-imgscalr-library.html

    for (w <- resolutions) {
      val resized = Scalr.resize(img, w); 
      val resizeFilename = file.getAbsolutePath.dropRight(4) + "-" + w + "." + extension
      val resizeFile = new File(resizeFilename)
      Logger.info("==== resized: " + resizeFilename)
      ImageIO.write(resized, extension, resizeFile)

      Image.create(new Image(0, resizeFile.getAbsolutePath, 
          Image.createUrl(ts + "/" + resizeFile.getName), restID, dishID, Some(userID), 
          Some(resized.getWidth), Some(resized.getHeight.toLong), status, null))
    }
    
  }
  
  def crop(id: Long, x: Int, y: Int, w: Int, h: Int) {
    val ts = System.currentTimeMillis
    Logger.info("==== cropping image: " + id  +  " x:" + x + " y:" + y + " w:" + w + " h:" + h)

    val original = findById(id)
    val file = new File(original.filename)
    val img = ImageIO.read(file); // load image
    val croppedImg = Scalr.crop(img, x, y, w, h)

    val extension = original.filename.takeRight(3).toLowerCase

    for (w <- resolutions) {
      val resized = Scalr.resize(croppedImg, w); 
      val resizeFilename = file.getAbsolutePath.dropRight(4) + "-" + w + "." + extension
      val resizeFile = new File(resizeFilename)
      Logger.info("==== resized: " + resizeFilename)
      ImageIO.write(resized, extension, resizeFile)

      //TODO this needs different switches
      Image.updateDishImages(original.restaurant_id, original.dish_id, original.status, original.user_id, 
          w, resized.getHeight.toLong, resizeFile.getAbsolutePath, 
          (original.url.take(original.url.lastIndexOf("/")) + "/" + resizeFile.getName))
    }
    
    Logger.info("==== DONE cropping " + (System.currentTimeMillis - ts))
  }

  def updateDishImages(restID: Long, dishID: Long, status: Long, userID: Option[Long], width: Long, 
      height: Long, filename: String, url: String) = {
    DB.withConnection { implicit connection =>
      SQL("update image set filename = {filename}, url = {url}, height = {height}, lastupdate = {lastupdate} " +
          "where dish_id = {dish_id} and restaurant_id = {restaurant_id} and user_id = {user_id} and width = {width} and status = {status}").on(
          'dish_id -> dishID,
          'restaurant_id -> restID,
          'user_id -> userID,
          'width -> width,
          'height -> height,
          'filename -> filename,
          'url -> url,
          'status -> status,
          'lastupdate -> new Date()).executeUpdate
    }
  }
  
  
  
  implicit val imageReads = Json.reads[Image]
  implicit val imageWrites = Json.writes[Image]

}

