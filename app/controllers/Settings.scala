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
import com.thoughtworks.xstream._
import models._
import views._
import common._
import models.json.NameValue
import models.json.RegisterResponse
import scala.util.Random
import models.json.TasteProfileDish

object Settings extends Controller with Secured {
  def load() = IsAuthenticated { username =>
    implicit request => {
      val me = User.findByEmail(username)
      
      val fullUser = User.getFullUser(username)
      
      val subdomainLanguage = request.headers.get(HeaderNames.ACCEPT_LANGUAGE).get/*.substring(0,2)*/
      Logger.debug(" language from request:" + subdomainLanguage)

      var userSettings = new UserSettings(fullUser.id, "en_US", false, "", 0, Seq.empty[Cuisine])
      if (fullUser.settings != null) {
        userSettings = Json.parse(fullUser.settings).validate[UserSettings].get 
        Logger.debug("parse ----->" + userSettings)
      }
      
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
      
      //TODO kinda wrong because commas in the done text, breaks everything
      //TODO simplified, but I want only starting 
      //TODO or better use a real library - i.e. http://code.google.com/p/opencsv/
    }
  }

  def generateXML = IsAuthenticated { username =>
    implicit request => {
      val xstream = new XStream
      val all = Restaurant.findAll()
      Ok(xstream.toXML(all))
    }
  }
  
  
  def getByUser(id: Long) = Action { 
    implicit request => {
      Logger.info("calling Activities get - load data for id:" + id)
      val all = User.getFullUser(id)
      all.password = null
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
      User.update(null, email, null) // TODO
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
      val all = Tag.findAll().filter(_.status == 21)
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
      
      val restaurantIDs = Restaurant.findAll().map { x => x.id }
      
      Logger.info("personalize taste profile parsed user_id:" + userId + "  favCuisines:" + favCuisines + "  preferToAvoid:" + preferToAvoid)
      
      val desiredWidth = 750
      val all = Random.shuffle(Dish.findAll).filter { x => restaurantIDs.contains(x.restaurant_id) }.take(5).map { dish => new TasteProfileDish(dish.id, 
          Image.findByDish(dish.id).filter{x => x.width.get == desiredWidth}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url, 
          Recommendation.makePriceString(dish.price), dish.name, dish.description.getOrElse("")) }
      
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
      
      Ok(Json.prettyPrint(Json.toJson(CommonJSONResponse.OK)))
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
      val id = (request.body.asJson.get \ "user_data" \ "id")
      
      val deviceOS = (request.body.asJson.get \ "device_operating_system")
      val deviceSWidth = (request.body.asJson.get \ "device_screen_width")
      val deviceLang = (request.body.asJson.get \ "device_language")
      
      val userByUsername = User.getFullUserByUsername(id.as[String])
      val userByPhone = User.getFullUserByPhone(cleanPhoneString(phone.as[String]))
      Logger.debug("userByUsername: " + userByUsername)
      Logger.debug("userByPhone: " + userByPhone)
      
      val existingUser = (userByUsername.isDefined || userByPhone.size > 0)
      
     
      val newid = existingUser match {
        case true => { if (userByUsername.isDefined) userByUsername.get.id else userByPhone(0).id } 
        case false => User.create(new UserFull(-1, null, null, false, "test", null, email.as[String], id.as[String], "1", null, name.as[String], null, null, null, cleanPhoneString(phone.as[String]))).get
      }
      
      val userStatus = existingUser match {
        case true => "existing_user"
        case false => "new"
      }

      //TODO also link URL!
      
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
}
