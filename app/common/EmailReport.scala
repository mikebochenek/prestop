package common

import java.text.SimpleDateFormat
import java.util.Date
import models._
import com.typesafe.plugin._
import play.api.Play.current
import models.MailLog
import play.api.Logger

object EmailReport {

  val sdf = new SimpleDateFormat("yyyyMMdd")
  val prettySdf = new SimpleDateFormat("EEE, dd MMM yyyy")

  val DAY = 24 * 60 * 60 * 1000;
  def inLastDay(aDate: Date) = {
    aDate.getTime() > System.currentTimeMillis() - DAY;
  }  
  
  def sendemail(user: UserFull) {
    Logger.debug ("processing sendemail to email:" + user.email + "  " + user.ttype)
    if ("7".equals(user.ttype) && (user.email.contains("sebastian") || user.email.contains("mike"))) {
  
      val dateStr = prettySdf.format(new Date())
  
      var html = "<html><body><h1>Stats " + dateStr + "</h1>"
  
      val body = views.html.dailyreports.render(Restaurant.findAll, 
          Dish.findAll.filter  { x => inLastDay(x.lastupdate) },
          Dish.findAllWithoutImages, 
          User.findAll.filter { x => inLastDay(x.createdate) }, 
          Friend.findAll.filter { x => inLastDay(x.lastupdate) }, 
          Reservation.findAll,
          ActivityLog.findRecentStats(7, 1),
          ActivityLog.findAll.filter { x => inLastDay(x.createdate) }).body
      
      Logger.debug(body)
      
      html += body
      
      html += "<hr/><code>" + models.AdminHelper.generateStats + "</code>"
      
      html += "</body></html>"
  
      val subject = "backend stats today: " + dateStr
  
      val mail = use[MailerPlugin].email
      mail.setSubject(subject)
      mail.setRecipient(user.email)
      mail.setFrom("info@idone.ch")
  
      Logger.info("about to send email to: " + user.email + " with " + html)
      if (isValid(user.email)) {
        mail.sendHtml(html)
        //MailLog.create(new MailLog(1, user.id, html, subject, null, -1))
      }
    }
  }

  /* http://stackoverflow.com/questions/13912597/validate-email-one-liner-in-scala
   * http://stackoverflow.com/questions/201323/using-a-regular-expression-to-validate-an-email-address */
  def isValid(email: String): Boolean = "^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$".r.unapplySeq(email).isDefined
  
}