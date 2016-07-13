import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class RecommendAPISpec extends Specification {

  "Application" should {

    "call recommend API" in new WithApplication{
      val home = route(FakeRequest(GET, "/api/recommend?id=1&longitude=47.3921582785799&latitude=8.512902108369&minPrice=0&maxPrice=150&maxDistance=20.0&openNow=false")).get
      Thread.sleep(20000)
      status(home) must equalTo(OK)
      contentAsString(home) must contain ("url")
      contentAsString(home) must contain ("score")
    }
  }
}
