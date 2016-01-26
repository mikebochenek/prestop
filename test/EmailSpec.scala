import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.User
import java.util.Date
import com.typesafe.plugin._
import play.api.Play.current
import actors.EmailJobActor
import akka.actor.{ Actor, Props }
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

    
@RunWith(classOf[JUnitRunner])
class EmailSpec extends Specification {

  "Application" should {

    "send one test email" in new WithApplication {
      val mail = use[MailerPlugin].email
      mail.setSubject("mailer test")
      mail.setRecipient("mike.bochenek@gmail.com")
      mail.setFrom("mike.bochenek@gmail.com")
      //mail.sendHtml("<html>html</html>")
    }
    
   "check email job" in new WithApplication {
      val actor = Akka.system.actorOf(Props(new EmailJobActor()))

//      Akka.system.scheduler.schedule(1.seconds, 1.days, actor, "send")
      
      //val myActor = Akka.system.actorOf(Props(classOf[EmailJobActor]), "case2-primary")
      //tester.send(fileActor, CreateFile(1))
    }

  }
}
