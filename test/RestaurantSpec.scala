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
    
    "be able to call google places API" in new WithApplication {
      val testIDs = Array( "ChIJs1urRqugmkcRA1ulmr2Q5wk", "ChIJOaw7shkKkEcRevsegZ7lc3g", "ChIJoSeh4w4w2jAR1yUxbme3kg3", "ChIJeY4ysm4KkEcRRr1vBg7RZHI",
          "ChIJs1urRqugmkcRA1ulmr2Q5wk", "ChIJST5PPAIKkEcRK8MasC5mGuA", "ChIJ89b43A8KkEcRO0Xx4rAWdjY", "ChIJ7e10j0AKkEcRJwqD0PC3zpo", 
          "ChIJq4p1lBoKkEcRd1lYBV7M5BA", "ChIJ2xL3d8gLkEcRxCMPd7US9W4", "ChIJEbKGHRsKkEcRk8MSw8joFTY", "ChIJRXOzSBsKkEcRZf8YNtrie7I",
          "ChIJMzBL-xoKkEcR6zmxgZUGgO0", "ChIJk8kDXxoKkEcR3ZY2CS8nLUA", "ChIJ0Y7N6xgKkEcRnWZjW3TcP3U", "ChIJraTDVgYKkEcRLTDxdibbmi4",
          "ChIJiQ4Iez8KkEcROx2vKtymAVA", "ChIJTQLwOqigmkcRM7eA90UJyKs", "ChIJZYfn7FoKkEcRP4_VMAUATAE", "ChIJYUI9ZRIKkEcRayiX2fykWQs",
          "ChIJdRYWqokKkEcRQMoHchSrRTY", "ChIJ-06cJgYKkEcRlN9bifjsY_g", "ChIJr5rzOqqgmkcRYqfno8OTC1c", "ChIJ55VuXCIKkEcRpHexd1eLPL0",
          "ChIJ6UzMahsKkEcRBFr87qCGS70", "ChIJwcvP9KegmkcRi0lMyqj6hyI", "ChIJhxFpiyIKkEcRKM0i9MdeAvM", "ChIJr5rzOqqgmkcRYqfno8OTC1c")
      for (t <- testIDs) {
        println(controllers.Restaurants.callGooglePlacesAPI(t))
      }
    }
  }
}
