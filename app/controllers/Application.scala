package controllers

import java.io.File
import net.tanesha.recaptcha._
import play.Play
import play.api.Play.current
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
import common.EmailReport

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
    Logger.debug("request.Content-Type   : " + request.headers.get("Content-Type"))
    Logger.debug("request.Accept-Charset : " + request.headers.get("Accept-Charset"))
    

    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => {
        val fullUser = User.getFullUser(user._1)
        ActivityLog.create(fullUser.get.id, ActivityLog.TYPE_LOGIN_ATTEMPT, 0, "")
        Redirect(routes.Restaurants.index).withSession("email" -> user._1, "usertype" -> fullUser.get.ttype)
      })
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

  val forgotPasswordForm = Form[(String, String, String)](
    tuple(
      "email" -> nonEmptyText,
      "recaptcha_challenge_field" -> nonEmptyText,
      "recaptcha_response_field" -> nonEmptyText))
  
  def forgotpassword = Action { implicit request =>
    Ok(html.forgotpassword(forgotPasswordForm, !current.configuration.getString("image.server.baseurl").getOrElse("").contains("localhost")))
  }
  
  def submitforgotpassword = Action { implicit request =>
    forgotPasswordForm.bindFromRequest.fold(
      failure => ( Redirect(routes.Application.forgotpassword).flashing("error" -> "Missing email address or captcha.") ),
      { case (email, q, a) => {
          if (checkCaptcha("addr", q, a)) { // hmmm... "addr" was a variable in their sample
            EmailReport.sendnewpassword(email)
            Redirect(routes.Application.login).withNewSession.flashing("success" -> "You can try to login once you get the email")
          } else {
            Redirect(routes.Application.forgotpassword).flashing("error" -> "Captcha is not correct.  Please try again.")
          }
        }
      }
    )
  }
  
  def publicKey(): String = {
    current.configuration.getString("recaptcha.publickey").getOrElse("6Lcpy9YSAAAAAKPK5T8tdO5WbiRPkKENziunk0c2")
  }
  def privateKey(): String = {
    current.configuration.getString("recaptcha.privatekey").getOrElse("6Lcpy9YSAAAAANlSJ-iw9GDSKFYX5HktGbs-oG7D")
  }
  def stripePrivateKey(): String = {
    current.configuration.getString("stripe.secretkey").getOrElse("")
  }
  
  def renderCaptcha(secure: Boolean): String = { 
    if (secure) {
      ReCaptchaFactory.newSecureReCaptcha(publicKey(), privateKey(), false).createRecaptchaHtml(null, new java.util.Properties)
    } else {
      ReCaptchaFactory.newReCaptcha(publicKey(), privateKey(), false).createRecaptchaHtml(null, new java.util.Properties)
    }
  }
  
  def checkCaptcha(addr: String, challenge: String, response: String): Boolean = {
    val reCaptcha = new ReCaptchaImpl()
    reCaptcha.setPrivateKey(privateKey())
    val reCaptchaResponse = reCaptcha.checkAnswer(addr, challenge, response)
    reCaptchaResponse.isValid()
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
