package common

import java.text.SimpleDateFormat
import java.util.Date
import models._
import com.typesafe.plugin._
import play.api.Play.current
import models.MailLog
import play.api.Logger
import scala.util.Random

object EmailReport {

  val sdf = new SimpleDateFormat("yyyyMMdd")
  val prettySdf = new SimpleDateFormat("EEE dd.MM.yyyy")

  val DAY = 24 * 60 * 60 * 1000;
  def inLastDay(aDate: Date) = {
    aDate.getTime() > System.currentTimeMillis() - DAY;
  }  
  
  def sendemail(user: UserFull) {
    Logger.debug ("processing sendemail to email:" + user.email + "  " + user.ttype)
    if ("7".equals(user.ttype) && (user.email.contains("sebastian") || user.email.contains("mike"))) {
  
      val dateStr = prettySdf.format(new Date())
  
      var html = "<html><body><h1>Stats for: " + dateStr + "</h1>"
  
      val body = views.html.dailyreports.render(Restaurant.findAll, 
          Dish.findAll.filter  { x => inLastDay(x.lastupdate) },
          Dish.findAllWithoutImages, 
          User.findAll.filter { x => inLastDay(x.createdate) }, 
          Friend.findAll.filter { x => inLastDay(x.lastupdate) }, 
          Reservation.findAll,
          ActivityLog.findRecentStats(7, 1),
          ActivityLog.findAll.filter { x => inLastDay(x.createdate) }).body
      
      html += body
      
      html += "</body></html>"
  
      val subject = "Presto backend stats for: " + dateStr
  
      val mail = use[MailerPlugin].email
      mail.setSubject(subject)
      mail.setRecipient(user.email)
      mail.setFrom("info@idone.ch")
  
      Logger.info("about to send email to: " + user.email + " with content len " + html.size)
      if (isValid(user.email)) {
        mail.sendHtml(html)
        //MailLog.create(new MailLog(1, user.id, html, subject, null, -1))
      }
    }
  }
  
  def sendnewpassword(email: String) = {
    val user = User.getFullUser(email)
    Logger.info (user.isDefined + " - generate new password for : " + email)
    if (user.isDefined) {
      val pwd = newpassword()
      
      val subject = "New Presto Admin Password for: " + email

      var html = "<html><body><h1>" + subject + "</h1>"
      html += "Hello " + user.get.fullname
      html += "<br>Your new password is: " + pwd
      html += "<br>You can change it after logging in."
      html += "</body></html>"
  
      val mail = use[MailerPlugin].email
      mail.setSubject(subject)
      mail.setRecipient(user.get.email)
      mail.setFrom("info@idone.ch")
  
      if (isValid(user.get.email)) {
        mail.sendHtml(html)
        
        User.updatepassword(user.get.email, pwd)
      }
    }
  }
  
  def newpassword() = {
    Random.alphanumeric take 10 mkString("")
  }

  /* http://stackoverflow.com/questions/13912597/validate-email-one-liner-in-scala
   * http://stackoverflow.com/questions/201323/using-a-regular-expression-to-validate-an-email-address */
  def isValid(email: String): Boolean = "^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$".r.unapplySeq(email).isDefined
  
}