import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.User

@RunWith(classOf[JUnitRunner])
class ImageSpec extends Specification {

  "Application" should {

    "be able to reverse URL " in new WithApplication{
      //val user = User.create(new User(2, "delme-" + System.currentTimeMillis(), "delme", "delme"))
      //user.id must greaterThan(0L)
    }
  }
}
