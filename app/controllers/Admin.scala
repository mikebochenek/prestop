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

object Admin extends Controller with Secured {
  def index() = IsAuthenticated { username =>
    implicit request => {
      val id = User.findByEmail(username).id
      if (username.contains("bochenek")) {
        val stats = AdminHelper.generateStats()
        Ok(views.html.iadmin(stats))
      } else {
        Ok(views.html.index())
      }
    }
  }
}
