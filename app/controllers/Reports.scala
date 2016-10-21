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
      Ok(views.html.reports(Restaurant.findAll, Dish.findAll, Dish.findAllDeleted, User.findAll, 
          Friend.findAll, Reservation.findAll, ActivityLog.findAll, ActivityLog.findRecentActivities,
          Image.countAll, AdminHelper.generateStats(), AdminHelper.deletedImages()))
    }
  }
  def loaddaily() = IsAuthenticated { username =>
    implicit request => {
      Ok(views.html.dailyreports(Restaurant.findAll, Dish.findAllInactive, Dish.findAllWithoutImages, User.findAll, 
          Friend.findAll, Reservation.findAll,
          ActivityLog.findRecentStats(7, 100),
          ActivityLog.findAll, Restaurant.findAll.filter { r => r.status == 4 }, Dish.findAll().filter { d => d.status == 4 }))
    }
  }
}
