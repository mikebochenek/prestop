import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import common.Recommendation
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import java.util.Date
import models.RecommendationFilter

@RunWith(classOf[JUnitRunner])
class RecommendationSpec extends Specification {

  "Application" should {

    "check schedule 1" in new WithApplication {
      val s = "Mo - Mi: 07:30 - 24:00\r\nDo - Fr: 07:30 ? 01:00\r\nSa: 09:00 ? 01:00\r\nSo: 10:00 ? 24:00   "
      println(new Date().toString + " --> " + Recommendation.checkSchedule(s) + "  schedule:" + s)    
    }
    
    "check schedule 2" in new WithApplication {
      val s = "Mon ? Sun: 11:30 ? 23:00 "
      println(new Date().toString + " --> " + Recommendation.checkSchedule(s) + "  schedule:" + s)    
    }

   "check schedule 3" in new WithApplication {
      val s = "Mon ? Sun: 10:30 bis 23:45"
      println(new Date().toString + " --> " + Recommendation.checkSchedule(s) + "  schedule:" + s)    
    }

   "check schedule 4" in new WithApplication {
      val s = "Mon ? Wed: 11:30 ? 14:00, 17:00 ? 01:00\r\nThu ? Fri: 11:30 ? 14:00, 17:00 ? 02:00\r\nSat: 17:00 ? 02:00 "
      println(new Date().toString + " --> " + Recommendation.checkSchedule(s) + "  schedule:" + s)    
    }

   "check schedule 5" in new WithApplication {
      val s = "Tue: 17:00 ? 22:00\r\nWed ? Fri: 11:30 ? 22:00\r\nSat: 10:00 ? 22:00\r\nSun: 10:00 ? 20:00 "
      println(new Date().toString + " --> " + Recommendation.checkSchedule(s) + "  schedule:" + s)    
    }
   
   
   "check isTime" in new WithApplication {
      println(" is time? " + Recommendation.isTime("21:00"))    
      println(" is time? " + Recommendation.isTime("7:00"))    
      println(" is time? " + Recommendation.isTime("7a:00"))    
    }

  }
}
