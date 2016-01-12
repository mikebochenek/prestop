import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.Restaurant
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._
import java.util.Date
import models.RecommendationFilter

@RunWith(classOf[JUnitRunner])
class RecommendationFilterSpec extends Specification {

  "Application" should {

    "create JSON" in new WithApplication {
      val rf = new RecommendationFilter(100, 0, 100, false)
      println(/*Json.prettyPrint*/(Json.toJson(rf)))
    }

  }
}
