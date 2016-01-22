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

object Reports extends Controller with Secured {
  def load() = IsAuthenticated { username =>
    implicit request => {
      Ok(views.html.reports(Restaurant.findAll, Dish.findAll, User.findAll, 
          Friend.findAll, Reservation.findAll, ActivityLog.findAll, 
          Image.findAll, AdminHelper.generateStats()))
    }
  }
}
