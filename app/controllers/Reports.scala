package controllers

import java.io.File
import java.util.Date

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
  def analytics() = IsAuthenticated { username =>
    implicit request => {
      val avgDailyUsers = ActivityLog.averageDailyDishViewers(30L)
      Logger.info("avgDailyUsers: " + avgDailyUsers)
      Ok(views.html.analytics(avgDailyUsers.toString))
    }
  }
  def load(mode: String) = IsAuthenticated { username =>
    implicit request => {
      if ("imagecleanup".equals(mode)) {
        //TODO also find images for users who had been permanently deleted
        Logger.info(mode + " starting \n\n\n")
        val di = Image.findAll().filter { x => x.status == -1 }
        val deleteCommands = di.map { x => "rm  '" + x.filename + "'"} // "'  '" + x.filename.replaceAll(".jpg", "_delme_.jpg") + "'"}
        deleteCommands.foreach(println)
        Logger.info(mode + " ending \n\n\n")
      }
      Ok(views.html.reports(Restaurant.findAll, Dish.findAll, Dish.findAllDeleted, User.findAll, 
          Friend.findAll, Reservation.findAll, ActivityLog.findAll, ActivityLog.findRecentActivities,
          Image.countAll, AdminHelper.generateStats(), "" /*AdminHelper.deletedImages()*/))
    }
  }
  def loaddaily() = IsAuthenticated { username =>
    implicit request => {
      Ok(views.html.dailyreports(Restaurant.findAll,
          Dish.findAll.filter  { x => inLastDay(x.lastupdate) },
          Dish.findAllWithoutImages, 
          User.findAll.filter { x => inLastDay(x.createdate) }, 
          Friend.findAll.filter { x => inLastDay(x.lastupdate) }, 
          Reservation.findAll,
          ActivityLog.findRecentStats(7, 1),
          ActivityLog.findAll.filter { x => inLastDay(x.createdate) },
          Restaurant.findAll.filter { r => r.status == 4 }, 
          Dish.findAll().filter { d => d.status == 4 }))
    }
  }
  val DAY = 24 * 60 * 60 * 1000;
  def inLastDay(aDate: Date) = {
    aDate.getTime() > System.currentTimeMillis() - DAY;
  }  
  
  def lookupEmail(id: Long) = {
    User.getFullUser(id).email
  }

  def getDishOwner(id: Long) = {
    val activityLogUpload = ActivityLog.findAllBySubType(ActivityLog.TYPE_DISH_UPLOAD, id)
    activityLogUpload.size match {
      case 1 => { User.getFullUser(activityLogUpload(0).user_id).email}
      case _ => "" 
    }
  }
  
  def getRestaurantName(id: Long) = {
    Restaurant.findById(null, id)(0).name
  }
  
  def getDishName(id: Long) = {
    Dish.findById(null, id)(0).name
  }
}
