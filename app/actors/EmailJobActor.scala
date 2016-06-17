package actors

import akka.actor.Actor
import models._
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
    play.api.Logger.info("executing send() in EmailJobActor again...")
    User.findAll.foreach(common.EmailReport.sendemail)
  }
}