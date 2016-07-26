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
import models._
import views._
import common.RecommendationUtils

object Application extends Controller {
  /** serve the index page app/views/index.scala.html */
  def index(any: String) = Action { implicit request =>
    Ok(views.html.index())
  }
  
  def permalink(id: String) = Action { implicit request =>
    val dish = Dish.findById(null, id.toLong)(0)
    dish.url = Image.findByDish(dish.id).filter{x => x.width.get == 750}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
    Ok(views.html.permalink("title", dish.url, dish.name, RecommendationUtils.makePriceString(dish.price)))
  }

  /** resolve "any" into the corresponding HTML page URI */
  def getURI(any: String): String = any match {
    case "main" => "/public/html/main.html"
    case "detail" => "/public/html/detail.html"
    case _ => "error"
  }

  /** load an HTML page from public/html */
  def loadPublicHTML(any: String) = Action {
    val projectRoot = Play.application().path()
    val file = new File(projectRoot + getURI(any))
    if (file.exists())
      Ok(scala.io.Source.fromFile(file.getCanonicalPath()).mkString).as("text/html");
    else
      NotFound
  }

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text) verifying ("invalidlogin", result => result match {
        case (email, password) => User.authenticate(email, password).isDefined
      }))

  //TODO obviously this will have to include 2nd password and a call to create
  // and error handling (email already in use...)
  val createUserForm = Form(
    tuple(
      "email" -> text,
      "password" -> text,
      "password2" -> text) verifying ("createuserfailed", result => result match {
        case (email, password, password2) => User.create(email, password, password2).isDefined
      }))

  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    Logger.info("request.Content-Type   : " + request.headers.get("Content-Type"))
    Logger.info("request.Accept-Charset : " + request.headers.get("Accept-Charset"))
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Restaurants.index).withSession("email" -> user._1, "usertype" -> User.getFullUser(user._1).get.ttype))
  }

  def authenticateTest(email: String) = Action { implicit request =>
    val user = User.authenticate(email, "test")
    Redirect(routes.Recommend.test).withSession("email" -> user.get.email, "usertype" -> User.getFullUser(user.get.email).get.ttype)
  }

  def createuser = Action { implicit request =>
    Ok(html.createuser(createUserForm))
  }

  def submitcreateuser = Action { implicit request =>
    createUserForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.createuser(formWithErrors)),
      user => Redirect(routes.Restaurants.index).withSession("email" -> user._1))
  }

  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out")
  }

}

/**
 * Provide security features
 */
trait Secured {

  /** Retrieve the connected user email.*/
  def username(request: RequestHeader) = request.session.get("email")

  /** Redirect to login if the user in not authorized. */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
  
  /** Action for authenticated users. */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }
}
