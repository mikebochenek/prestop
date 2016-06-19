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
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json.JsArray
import play.api.libs.json._
import com.thoughtworks.xstream._
import models._
import views._
import common._
import models.json.NameValue
import models.json.RegisterResponse
import scala.util.Random
import models.json.TasteProfileDish
import scala.collection.mutable.MutableList
import play.api.http._
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.iteratee._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Codec, MultipartFormData }
import java.io.{FileInputStream, ByteArrayOutputStream}

object Settings extends Controller with Secured {
  def load() = IsAuthenticated { username =>
    implicit request => {
      val me = User.findByEmail(username)
      
      val fullUser = User.getFullUser(username)
      
      val subdomainLanguage = request.headers.get(HeaderNames.ACCEPT_LANGUAGE).get/*.substring(0,2)*/
      Logger.debug(" language from request:" + subdomainLanguage)

      val userSettings = getPreviousSettingsSafely(fullUser)
      
      val url = Image.findByUser(fullUser.id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url

      Ok(views.html.settings(settingsForm, me, userSettings, url))
    }
  }

  val settingsForm = Form(
    tuple(
      "email" -> text,
      "language" -> text,
      "password" -> text,
      "passwordnew1" -> text,
      "passwordnew2" -> text,
      "additionalsettings" -> text))

  def save = IsAuthenticated { username =>
    implicit request => { 
      val (email, language, password, passwordnew1, passwordnew2, additionalsettings) = settingsForm.bindFromRequest.get

      Logger.debug("email:" + email + " language:" + language)

      val fullUser = User.getFullUser(username)
      
      if (fullUser.settings == null || !additionalsettings.equals(fullUser.settings)) {
        Logger.debug("additionalsettings different from what we have in the DB: " + additionalsettings)
        User.update(fullUser, email, additionalsettings)
      }

      if (fullUser.settings != null) {
        val previousSettings = Json.parse(fullUser.settings).validate[UserSettings].get 
        if (!language.equals(previousSettings.language)) {
          Logger.debug("yes, we will save the language:" + language + "  was:" + previousSettings.language)

          previousSettings.language = language
          val settingsJson = Json.toJson(previousSettings).toString
          User.update(fullUser, email, settingsJson)
          
        }
      }
      
      //Logger.debug("password:" + password + " passwordnew1:" + passwordnew1 + " passwordnew2:" + passwordnew2)

      if (password != null && password.trim.length > 0 
          && passwordnew1 != null && passwordnew1.trim.length > 0 && passwordnew1.equals(passwordnew2)) {
        Logger.info("changing password for email:" + username)
        if (username.equals(User.authenticate(username, password).get.email)) {
          User.updatepassword(username, passwordnew1)
        }
      } //TODO only handles happy path for now


      Redirect(routes.Settings.load)
    }
  }

  def generateJSON = IsAuthenticated { username =>
    implicit request => {
      val all = Restaurant.findAll()
      Ok(Json.toJson(all.map(a => Json.toJson(a))))
    }
  }

  def generateCSV = IsAuthenticated { username =>
    implicit request => {
      val all = Restaurant.findAll()
      val csvstr = all.mkString("\n")
      val header = "id,owner,donetext,donedate,createdate,deleted,category,doneDay\n"

      Ok(header + csvstr.replaceAll("Done[(]", "").replaceAll("[)]", "")) 
      
      // kinda wrong because commas in the done text, breaks everything, simplified, but I want only starting 
      // or better use a real library - i.e. http://code.google.com/p/opencsv/
    }
  }

  def generateXML = IsAuthenticated { username =>
    implicit request => {
      val fullUser = User.getFullUser(username) //TODO instead of generating XML we send email as a test only!
      common.EmailReport.sendemail(fullUser)
      Ok(Json.toJson(models.CommonJSONResponse.OK))
    }
  }
  
  
  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling Activities get - load data for id:" + id)
      val all = getPreviousSettingsSafely(User.getFullUser(id))
      Ok(Json.prettyPrint(Json.toJson(all)))
    }
  } 

  def createUser() = Action {
    implicit request => {
      val email = (request.body.asJson.get \ "email").as[String]
      val username = (request.body.asJson.get \ "username").as[String]
      val id = User.create(email, null, null)
      Logger.info("create user - id: " + id)
      Ok("ok")
    }
  }
  
  def updateUser() = Action {
    implicit request => {
      val email = (request.body.asJson.get \ "email").as[String]
      val username = (request.body.asJson.get \ "username")
      val id = 13; 
      //User.update(null, email, null) // TODO
      Logger.info("TODO:  nothing has been created yet - " + email + " with id:" + id)
      Ok("ok")
    }
  }

  def uploadPhoto(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Logger.info("upload user photo " + id)
      Image.saveAndResizeImages(picture, id, "user")
      Redirect(routes.Settings.load)
    }.getOrElse {
      Redirect(routes.Restaurants.about).flashing(
        "error" -> "Missing file")
    }
  } 
  
  def cuisines() = Action { 
    implicit request => {
      Logger.info("calling get cuisines")
      val all = Tag.findAllPopular(21)
      Ok(Json.prettyPrint(Json.toJson(all)))
    }
  } 
  
  def cleanPhoneString(p: String) = {
    p.replaceAll("[^\\+\\d]", "") //http://stackoverflow.com/questions/1533659/how-do-i-remove-the-non-numeric-character-from-a-string-in-java
  }

  
  def personalizeTasteProfile() = Action {
    implicit request => {
      Logger.info("personalize taste profile - body:" + request.body.asJson)
      
      val userId = (request.body.asJson.get \ "user_id" )
      val favCuisines = (request.body.asJson.get \ "favCuisines" ) 
      val preferToAvoid = (request.body.asJson.get \ "preferToAvoid" )
      val sampleDishLikes = (request.body.asJson.get \ "sampleDishLikes" ) // this should always be empty

      smartInsertUpdate(userId, favCuisines, preferToAvoid, sampleDishLikes)

      val restaurantIDs = Restaurant.findAll().map { x => x.id }
      
      Logger.info("personalize taste profile parsed user_id:" + userId + "  favCuisines:" + favCuisines + "  preferToAvoid:" + preferToAvoid)
      
      val desiredWidth = 750
      val all = Random.shuffle(Dish.findAll).filter { x => restaurantIDs.contains(x.restaurant_id) }.take(3).map { dish => new TasteProfileDish(dish.id, 
          Image.findByDish(dish.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
          RecommendationUtils.makePriceString(dish.price), dish.name, dish.description.getOrElse("")) }
      
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  }  

  def personalize() = Action {
    implicit request => {
      Logger.info("personalize user - body:" + request.body.asJson)
      
      val userId = (request.body.asJson.get \ "user_id" )
      val favCuisines = (request.body.asJson.get \ "favCuisines" )
      val preferToAvoid = (request.body.asJson.get \ "preferToAvoid" )
      val sampleDishLikes = (request.body.asJson.get \ "sampleDishLikes" )
      
      Logger.info("personalize parsed user_id:" + userId + "  favCuisines:" + favCuisines + "  preferToAvoid:" + preferToAvoid + "  sampleDishLikes:" + sampleDishLikes)

      smartInsertUpdate(userId, favCuisines, preferToAvoid, sampleDishLikes)
      
      Ok(Json.prettyPrint(Json.toJson(CommonJSONResponse.OK)))
    }
  }
  
  def getPreviousSettingsSafely(fullUser: UserFull) = {
    fullUser.settings match {
      case null => UserSettings.default(fullUser.id)
      case _ => {
        try {
          Json.parse(fullUser.settings).validate[UserSettings].get
        } catch {
          case e: Exception => {
            Logger.info("failed to parse settings, will use default instead " + e)
            UserSettings.default(fullUser.id)
          }
        }
      }
    }
  }

  def smartInsertUpdate(userId: JsValue, favCuisines: JsValue, preferToAvoid: JsValue, sampleDishLikes: JsValue) = {
      val fullUser = User.getFullUser(userId.as[String].toLong)
      val previousSettings = getPreviousSettingsSafely(fullUser)
      
      if (previousSettings != null) {
        
        if (!favCuisines.isInstanceOf[JsUndefined]) {
          Logger.debug("yes, we will save the favCuisines:" + favCuisines + "  was:" + previousSettings.favCuisines)
          previousSettings.favCuisines = MutableList.empty[Cuisine];
          previousSettings.favCuisines ++= favCuisines.as[JsObject].fields.collect { case (key, JsString(value)) => new Cuisine(key, Option(value.toInt)) }
        }
        
        if (!preferToAvoid.isInstanceOf[JsUndefined]) {
          Logger.debug("yes, we will save the preferToAvoid:" + preferToAvoid + "  was:" + previousSettings.preferToAvoid)
          previousSettings.preferToAvoid = Option(MutableList.empty[Cuisine]);
          previousSettings.preferToAvoid.get ++= preferToAvoid.as[JsObject].fields.collect { case (key, JsString(value)) => new Cuisine(key, Option(value.toInt)) }
        }

        if (!sampleDishLikes.isInstanceOf[JsUndefined]) {
          Logger.debug("yes, we will save the sampleDishLikes:" + sampleDishLikes + "  was:" + previousSettings.sampleDishLikes)
          previousSettings.sampleDishLikes = Option(MutableList.empty[Cuisine])
          previousSettings.sampleDishLikes.get ++= sampleDishLikes.as[JsObject].fields.collect { case (key, JsString(value)) => new Cuisine(key, Option(value.toInt)) }
        }
        
        val settingsJson = Json.toJson(previousSettings).toString
        User.update(fullUser, fullUser.email, settingsJson)
      }
    
  }
  
  def register() = Action {
    implicit request => {
      Logger.info("register user - body:" + request.body.asJson)
      
      val email = (request.body.asJson.get \ "user_data" \ "email")
      val phone = (request.body.asJson.get \ "user_data" \ "phone_numer")
      val url = (request.body.asJson.get \ "user_data" \ "profile_pictureURL")
      val gender = (request.body.asJson.get \ "user_data" \ "gender")
      val name = (request.body.asJson.get \ "user_data" \ "name")
      val idJSON = (request.body.asJson.get \ "user_data" \ "id")
      val id = (idJSON.isInstanceOf[JsUndefined]) match {
        case true => ""
        case false => idJSON.as[String]
      }
      
      val deviceOS = (request.body.asJson.get \ "device_operating_system")
      val deviceSWidth = (request.body.asJson.get \ "device_screen_width")
      val deviceLang = (request.body.asJson.get \ "device_language") 
      
      val userByUsername = User.getFullUserByUsername(id)
      val userByPhone = User.getFullUserByPhone(cleanPhoneString(phone.as[String]))
      Logger.debug("userByUsername: " + userByUsername)
      Logger.debug("userByPhone: " + userByPhone)
      
      val existingUser = ((id.size > 0 && userByUsername.isDefined) || (userByPhone.size > 0 && cleanPhoneString(phone.as[String]).size > 0))
      
      val emailString = email.isInstanceOf[JsUndefined] match { 
        case true => ""
        case false => email.as[String]
      }
      val newid = existingUser match {
        case true => { if (userByUsername.isDefined) userByUsername.get.id else userByPhone(0).id } 
        case false => User.create(new UserFull(-1, null, null, false, "test", null, emailString, id, "1", null, name.as[String], null, null, null, cleanPhoneString(phone.as[String]))).get
      }
      
      val userStatus = existingUser match {
        case true => "existing_user"
        case false => "new"
      }

      // download image and create link URL!
      val filename = "/tmp/user" + newid + "-" + System.currentTimeMillis + ".jpg" //TODO make generic and more testing probably needed
      FileDownloader.download(url.as[String], filename)
      Image.saveAndResizeImages(FilePart("qqfile", "user" + newid + ".jpg", Some("image/jpeg"), TemporaryFile(new File(filename))), newid, "user") 
      //TODO cleanup work
      
      val fullUser = User.getFullUser(newid)
      
      if (cleanPhoneString(phone.as[String]).size > 0) { // only update the phone number when we have something valid
        fullUser.phone = cleanPhoneString(phone.as[String])
      }
      
      val previousSettings = getPreviousSettingsSafely(fullUser)

      if (!deviceOS.isInstanceOf[JsUndefined]) { previousSettings.deviceOS = Option(deviceOS.as[String]) }
      if (!deviceSWidth.isInstanceOf[JsUndefined]) { previousSettings.deviceSWidth = Option(deviceSWidth.as[String].toDouble) }
      if (!deviceLang.isInstanceOf[JsUndefined]) { previousSettings.deviceLang = Option(deviceLang.as[String]) }
        
      User.update(fullUser, fullUser.email, Json.toJson(previousSettings).toString)
      
      
      Logger.info("parsed name: " + name + " id: " + id + " email: " + email + " phone: " + phone + " gender:" + gender + " url: " + url)
      Logger.info("found / created new user with id: " + newid + " " + userStatus)
      
      Ok(Json.prettyPrint(Json.toJson(new RegisterResponse("OK", newid, userStatus))))
    }
  }
  
  def introTexts(lang: String) = Action {
    implicit request => {
      val tags = Tag.findAll().filter { x => x.status == Tag.TYPE_INTRO_TEXTS }
      Logger.info("intro texts for lang:" + lang + "  count:" + tags.size)
      lang match {
        case "de" => Ok(Json.prettyPrint(Json.toJson(tags.map { x => Json.obj(x.name -> x.de_text) })))
        case "it" => Ok(Json.prettyPrint(Json.toJson(tags.map { x => Json.obj(x.name -> x.it_text) })))
        case "fr" => Ok(Json.prettyPrint(Json.toJson(tags.map { x => Json.obj(x.name -> x.fr_text) })))
        case default => Ok(Json.prettyPrint(Json.toJson(tags.map { x => Json.obj(x.name -> x.en_text) })))
      } 
    }
  }
  
  def deleteUser(id: Long) = Action {
    implicit request => {
      Logger.info("deleting user: " + id)
      val activityDel = ActivityLog.delete(id)
      val friendDel = Friend.deleteByUserId(id)
      val friendFDel = Friend.deleteByFriendUserId(id)
      val userDel = User.delete(id)
      Logger.info("deleted " + activityDel + " activities, " + friendDel + " friends, " + friendFDel + " ffriends, " + userDel + " users")
      Ok(Json.prettyPrint(Json.toJson(CommonJSONResponse.OK)))
    }
  }
  
  def userList() = Action {
    implicit request => {
      Logger.info("userList")
      Ok(views.html.userlist_edit(User.findAll))
    }
  }
  
  def userEdit(id: Long) = Action {
    implicit request => {
      Logger.info("userEdit: " + id)
      val user = User.getFullUser(id)
      Ok(views.html.user_edit(userForm, user, getPreviousSettingsSafely(user), 
          ActivityLog.findRecentByUserType(id, 7), ActivityLog.findRecentByUserType(id, 11)))
    }
  }

  val userForm = Form(
    tuple(
      "id" -> text,
      "email" -> text,
      "username" -> text,
      "fullname" -> text,
      "phone" -> text,
      "city" -> text,
      "settings" -> text))
      
  def saveUser() = IsAuthenticated { username =>
    implicit request =>  
      val (id, email, username, fullname, phone, city, settings) = userForm.bindFromRequest.get
      Logger.info("calling saveUser for id: " + id + " email: " + email)
      val user = User.getFullUser(id.toInt)
      val newUser = new UserFull(user.id, user.createdate, user.lastlogindate, user.deleted, user.password, 
          settings, email, username, user.ttype, user.openidtoken, fullname, city, user.state, user.country, phone)
      User.update(newUser, email, settings)
      Redirect(routes.Settings.userEdit(id.toLong))
  }  
  
}
