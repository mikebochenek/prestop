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

object Settings extends Controller with Secured {
  def load() = IsAuthenticated { username =>
    implicit request => {
      val me = User.findByEmail(username)
      
      val fullUser = User.getFullUser(username)
      
      val subdomainLanguage = request.headers.get(HeaderNames.ACCEPT_LANGUAGE).get/*.substring(0,2)*/
      Logger.debug(" language from request:" + subdomainLanguage)

      var userSettings = new UserSettings(fullUser.id, "en_US", false, "")
      if (fullUser.settings != null) {
        userSettings = Json.parse(fullUser.settings).validate[UserSettings].get 
        Logger.debug("parse ----->" + userSettings)
      }
      
      Ok(views.html.settings(settingsForm, me, userSettings))
    }
  }

  val settingsForm = Form(
    tuple(
      "email" -> text,
      "language" -> text,
      "password" -> text,
      "passwordnew1" -> text,
      "passwordnew2" -> text,
      "newusertarget" -> text))

  def save = IsAuthenticated { username =>
    implicit request => { 
      val (email, language, password, passwordnew1, passwordnew2, newusertarget) = settingsForm.bindFromRequest.get

      Logger.debug("email:" + email + " language:" + language)

      val fullUser = User.getFullUser(username)

      if (fullUser.settings == null || language != Json.parse(fullUser.settings) \ "language") {
        Logger.debug("yes, we will save the language:" + language)

        val userSettings = new UserSettings(fullUser.id, language, false, "")
        val settingsJson = Json.toJson(userSettings).toString
        Logger.debug(settingsJson)
        User.update(fullUser, email, settingsJson)
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
      import java.io.File
      val filename = picture.filename
      val contentType = picture.contentType
      val ts = System.currentTimeMillis()
      val path = "/home/mike/data/presto/" + ts
      val file = new File(path + s"/$filename")
      picture.ref.moveTo(file)
      //Image.updateDishImages(id, -1)
      Image.create(new Image(0, file.getAbsolutePath, Image.createUrl(ts + "/" + file.getName), 0, id, 0, null))
      Redirect(routes.Dishes.getById(id))
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
}
