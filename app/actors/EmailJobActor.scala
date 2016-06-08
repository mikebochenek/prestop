package actors

import akka.actor.Actor
import models._
import java.text.SimpleDateFormat
import java.util.Date
import com.typesafe.plugin._
import play.api.Play.current
import models.MailLog
import play.api.Logger

class EmailJobActor() extends Actor {
  def receive = {
    case "send" => {
      send()
    }
    case _ => play.api.Logger.warn("unsupported message type")
  }

  def send() {
    play.api.Logger.info("executing send() in EmailJobActor..")
    User.findAll.foreach(sendemail)
  }

  val sdf = new SimpleDateFormat("yyyyMMdd")
  val prettySdf = new SimpleDateFormat("EEE, dd MMM yyyy")

  def sendemail(user: UserFull) {
    Logger.info ("processing sendemail to email:" + user.email)

    val dateStr = prettySdf.format(new Date())

    var html = "<html><body><h1>Stats " + dateStr + "</h1>" + "<ul>"


    html += "</ul></body></html>"

    val subject = "backend stats today: " + dateStr

    val mail = use[MailerPlugin].email
    mail.setSubject(subject)
    mail.setRecipient(user.email)
    mail.setFrom("info@bochenek.ch")

    Logger.info("about to send email to: " + user.email + " with " + html)
    if (isValid(user.email)) {
      mail.sendHtml(html)
      MailLog.create(new MailLog(1, user.id, html, subject, null, -1))
    }
  }

  /* http://stackoverflow.com/questions/13912597/validate-email-one-liner-in-scala
   * http://stackoverflow.com/questions/201323/using-a-regular-expression-to-validate-an-email-address */
  def isValid(email: String): Boolean = "^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,4})$".r.unapplySeq(email).isDefined
}