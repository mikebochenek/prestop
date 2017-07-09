import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.User
import speech.TwilioController

@RunWith(classOf[JUnitRunner])
class TwilioSpec extends Specification {

  "Application" should {

    "be able to create final prompt for Twilio" in new WithApplication{
      val xml = TwilioController.createFinalPrompt().toXml()
      System.out.println("createFinalPrompt: " + xml)
      xml must contain ("<Response>")
    }

    "be able to create first prompt for Twilio" in new WithApplication{
      val xml = TwilioController.createFirstPrompt().toXml()
      System.out.println("createFirstPrompt: " + xml)
      xml must contain ("<Response>")
    }

    "be able to extract date from string to Twilio" in new WithApplication{
      val dateString = TwilioController.extractTime(Option("Monday July 11th at 8:00pm"))
      System.out.println("extractTime: " + dateString)
    }
    
  }
}
