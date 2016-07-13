import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models.Restaurant
import java.util.Date

@RunWith(classOf[JUnitRunner])
class RestaurantSpec extends Specification {

  "Application" should {

    "create restaurant entries" in new WithApplication {
      val id = Restaurant.create("name", "city", "street", 101.0, -83.3, "schedule x", 12, Option(0), 4);
      println ("creating new restaurant with id:" + id)
    }

    "update restaurant entry" in new WithApplication {
      val id = Restaurant.update(2, "burgers", "zurich", "kapstr 4", 32.4, -13.42, "xes", 13, 1, "123", "tat", null, null, null, null, null);
      println ("updating existing restaurant with id:" + id)
    }

    "count restaurants" in new WithApplication {
      val count = Restaurant.countAll()
      println("restaurant count:" + count)
    }
    
    "find all restaurants" in new WithApplication {
      val all = Restaurant.findAll()
      println("restaurant count:" + all.size)
    }
  }
}
