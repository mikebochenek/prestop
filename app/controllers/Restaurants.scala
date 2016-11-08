package controllers

import models._
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Session
import views.html
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import java.util.Date
import java.util.Calendar
import play.api.Logger
import models.Tag
import models.RestaurantFriends
import models.Reservation
import models.User
import models.RestaurantOwner
import models.UserFull
import models.RestaurantMiscInfo
import common.RecommendationUtils
import models.json.GooglePlacesResponseResult
import models.json.GooglePlacesResponse

object Restaurants extends Controller with Secured {

  def index = Action { implicit request =>
    Ok(html.restaurants());
  }

  def about = Action { implicit request =>
    Ok(html.about());
  }

  def contact = Action { implicit request =>
    Ok(html.contact());
  }

  def createAndPopulate(id: Long, longitude: String, latitude: String, name: String, street: String, postalcode: String, 
      phone: String, website: String, schedule: String, place_id: String) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling restaurant edit AND populate - load data for id:" + id)
      val newid = _create(name, username);
      Logger.info("restaurant created with " + name + " with id:" + newid)
      
      val misc = new RestaurantMiscInfo(Option(postalcode), Option(""), Option(website), Option("Switzerland"), 
          Option(0), Option(place_id), null)
      val r = new Restaurant(newid.get, name, "", street, longitude.toDouble, latitude.toDouble, 
          schedule.replaceAll(":::", "\n"), false, 0, 0, Option("+"+phone.trim), Option(""), Option(postalcode), 
          Option("Zürich"), Option(website), null, null, Seq.empty[String], Seq.empty[String],
          Seq.empty[RestaurantFriends], misc)
      
      Ok(views.html.restaurant_edit(restaurantForm, r, "", "", "", "", 
          Reservation.findAllByRestaurant(id), Restaurant.findAllByParent(id)))
    }
  }

  /**
   * called from /api/restaurant/:id
   */
  def getById(id: Long) = Action {
    implicit request => {
      val all = Restaurant.findById(null, id)
      for (restaurant <- all) {
        restaurant.paymentoptions =  Tag.findByRef(restaurant.id, 12).map(_.en_text.get) //TODO good, but this has to be dynamic somehow
        restaurant.cuisines =  Tag.findByRef(restaurant.id, 21).map(_.en_text.get) //TODO should be dynamic
        val defaultImage = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 750 && x.status == 0}
          .headOption.getOrElse(Image.blankImage).asInstanceOf[Image]
        restaurant.url = defaultImage.url
        restaurant.smallurl = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 72 && x.status == 1}
          .headOption.getOrElse(defaultImage).asInstanceOf[Image].url
        
        restaurant.open_now = common.RecommendationUtils.checkSchedule(restaurant.schedule)
        
        if (restaurant.city != null) {
          restaurant.city = restaurant.city + ", Switzerland" //TODO remove this when we add country field
        }
        
        val friends = List(new RestaurantFriends(1, "Mike Bochenek", 
            Image.findByUser(1).filter{x => x.width.get == 72}.headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url))
        restaurant.friendsWhoBooked = friends;
        
        try {
          ActivityLog.create(1, 9, id, "") //TODO we don't have userID
        } catch {
          case _: Throwable => Logger.error("Could not create activity log for api/restuarant, but thats OK")
        }
      }
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  }

  def callGooglePlacesAPI(id: String) = {
    Logger.info(".. looks like need to populate restaurant info from Google Places: " + id)
    val url = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + id + 
              "&key=AIzaSyCmRHTsV4bmezqHapCyv3kHSIW6qxwVTCM" //TODO move key to properties
    val resp = io.Source.fromURL(url).mkString
    val googlePlaces = GooglePlacesResponse.getInstance(resp)
    googlePlaces
  }
  
  def edit(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling restaurant edit - load data for id:" + id)
      val user = User.getFullUser(username).get
      val all = "7".equals(user.ttype) match {
        case true => Restaurant.findById(username, id)
        case false => Restaurant.findAllByUser(user.id).filter { x => x.id == id }
      } 
      
      if (all(0).name.equals(all(0).misc.place_id.getOrElse("")) && all(0).name.length == 27) { // NB: because Dishes.uploadDish makes them equal
        val googlePlaces = callGooglePlacesAPI(all(0).misc.place_id.get)
        if (googlePlaces.result.name.length > 1) {
          all(0).name = googlePlaces.result.name
          all(0).website = Option(googlePlaces.result.website)
          all(0).misc.website = Option(googlePlaces.result.website)
          all(0).latitude = googlePlaces.result.geometry.get.location.lat
          all(0).longitude = googlePlaces.result.geometry.get.location.lng
          all(0).phone = Option(googlePlaces.result.international_phone_number)
        }
      }
      
      val tags = Tag.findByRef(id, 12).map(_.name).mkString(", ")
      val cuisines = Tag.findByRef(id, 21).map(_.name).mkString(", ")
      val url = Image.findByRestaurant(id).filter { x => x.status == 0 }.sortBy{ _.id }
        .headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      val logourl = Image.findByRestaurant(id).filter { x => x.status == 1 }.sortBy{ _.id }
        .headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      val reservations = Reservation.findAllByRestaurant(id)
      val childRestaurants = Restaurant.findAllByParent(id)
      
      if ("".equals(all(0).phone.getOrElse(""))) { all(0).phone = Option("+41") }
      if ("".equals(all(0).city)) { all(0).city = "Zürich" }
      if ("".equals(all(0).state.getOrElse(""))) { all(0).state = Option("Zürich") }
      if ("".equals(all(0).misc.country.getOrElse(""))) { all(0).misc.country = Option("Switzerland") }
      Ok(views.html.restaurant_edit(restaurantForm, all(0), url, logourl, tags, cuisines, reservations, childRestaurants))
    }
  }

  val restaurantForm = Form(
    tuple(
      "id" -> text,
      "name" -> text,
      "phone" -> text,
      "email" -> text,
      "address" -> text,
      "city" -> text,
      "postalcode" -> text,
      "state" -> text,
      "country" -> text,
      "website" -> text,
      "latitudelongitude" -> text,
      "schedule" -> text,
      "restype" -> text,
      "status" -> text,
      "ptags" -> text,
      "google_places_id" -> text,
      "ctags" -> text))

  def save = IsAuthenticated { username =>
    implicit request => { 
      var validationErrors = ""
      val (id, name, phone, email, address, city, postalcode, state, country, website, latitudelongitude, 
          schedule, restype, status, ptags, google_places_id, ctags) = restaurantForm.bindFromRequest.get
      
      if (!latitudelongitude.contains(",")) {
        validationErrors += "Should contain double,double"
      } 
      val ll = latitudelongitude.split(",")
      val latitude = (ll(0).size match {
        case 0 => "0"
        case default => ll(0)
      }).toDouble
      val longitude = (ll.size match {
        case 2 => ll(1) 
        case default => "0"
      }).toDouble
      
      val fullUser = User.getFullUser(username).get
      val newStatus = ("7".equals(fullUser.ttype) || status.toInt == -1) match {
        case true => status.toInt
        case false => 4
      }
      
      if (validationErrors.length() == 0) {
        Restaurant.update(id.toLong, name, city, address, longitude, latitude, schedule, restype.toInt, newStatus, 
          phone, email, postalcode, state, country, website, google_places_id)
        Tag.updateTags(id.toLong, ptags, 12)
        Tag.updateTags(id.toLong, ctags, 21)
        Logger.info("calling restaurant update for id:" + id + " newStatus:" + newStatus) 
        Redirect(routes.Restaurants.edit(id.toLong)).flashing("success" -> ("Changes saved successfully at " + RecommendationUtils.currentTime()))
      } else {
        Logger.error("failed calling restaurant update for id:" + id + " validationErrors:" + validationErrors) 
        Redirect(routes.Restaurants.edit(id.toLong)).flashing("error" -> validationErrors)
      }
    }
  }
  
  def getRestaurantsForUser(user: UserFull): Seq[Restaurant] = user.ttype match {
    case "7" => Restaurant.findAll()
    case _   => Restaurant.findAllByUser(user.id)
  }

  def getAll() = IsAuthenticated { username =>
    implicit request => {
      val user = User.getFullUser(username).get
      val all = getRestaurantsForUser(user)
      for (restaurant <- all) {
        restaurant.paymentoptions =  Tag.findByRef(restaurant.id, 12).map(_.name)
        restaurant.cuisines =  Tag.findByRef(restaurant.id, 21).map(_.name)
        restaurant.url = Image.findByRestaurant(restaurant.id).filter{x => x.width.get == 750}
          .headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      }
      Ok(Json.prettyPrint(Json.toJson(all.map(a => Json.toJson(a)))))
    }
  } 

  def _create(txt: String, username: String) = {
    val user = User.getFullUser(username).get
    val newStatus = "7".equals(user.ttype) match {
      case true => 0
      case false => 4
    } 
      
    val id = Restaurant.create(txt, "", "", 0.0, 0.0, "", 0, None, newStatus);
      
    if (newStatus == 4 && id.isDefined) {
      val ownerId = Restaurant.createOwner(id.get, user.id, "")
      Logger.info("restaurant owner created for id: " + ownerId + " restaurant_id: " + id + " user: " + user.id)
    }
      
    Logger.info("restaurant created with " + txt + " with id:" + id)
    id
  }
  
  def create() = IsAuthenticated { username =>
    implicit request => {
      val txt = (request.body.asJson.get \ "donetext")
      
      _create(txt.as[String], username)
      
      Ok("ok")
    }
  }
  
  def upload(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      Image.saveAndResizeImages(picture, id, "restaurant")
      Redirect(routes.Dishes.cropImage(id, "restaurant"))
    }.getOrElse {
      Redirect(routes.Restaurants.edit(id)).flashing(
        "error" -> "Missing file")
    }
  }  
  
  def uploadLogo(id: Long) = Action(parse.multipartFormData) { request =>
    request.body.file("logopicture").map { picture =>
      Image.saveAndResizeImages(picture, id, "restaurantlogo")
      Redirect(routes.Dishes.cropImage(id, "restaurantProfile"))
    }.getOrElse {
      Redirect(routes.Restaurants.edit(id)).flashing(
        "error" -> "Missing file")
    }
  }  

  val restaurantLocationsForm = Form(
    tuple(
      "id" -> text,
      "phone" -> text,
      "email" -> text,
      "address" -> text,
      "city" -> text,
      "postalcode" -> text,
      "state" -> text,
      "longitude" -> text,
      "latitude" -> text,
      "schedule" -> text,
      "status" -> text))
  
  def saveLocations = IsAuthenticated { username =>
    implicit request => { 
      val (id, phone, email, address, city, postalcode, state, longitude, latitude, schedule, status) = restaurantLocationsForm.bindFromRequest.get
      Logger.info("calling restaurant locations edit - load data for id:" + id)
      val rest = Restaurant.findById(username, id.toLong)(0)
      Restaurant.update(id.toLong, rest.name, city, address, longitude.toDouble, latitude.toDouble, schedule, rest.restype, status.toInt, phone, email, postalcode, state, rest.misc.country.getOrElse(""), rest.website.getOrElse(""), rest.misc.place_id.getOrElse(""))
      Logger.info("done calling restaurant CHILD update for id:" + id)
      Ok(views.html.restaurant_locations(restaurantLocationsForm, Restaurant.findById(username, id.toLong)(0)))
    }
  }
  
  def editLocations(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling restaurant locations load - for id:" + id)
      val all = Restaurant.findById(username, id.toLong)
      Ok(views.html.restaurant_locations(restaurantLocationsForm, all(0)))
    }
  }

  def addLocations(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling add new location - for id:" + id)
      val r = Restaurant.findById(username, id)(0)
      val newid = Restaurant.create(r.name, r.city, r.address, r.longitude, r.latitude, r.schedule, r.restype, Option(r.id), r.status);
      Redirect(routes.Restaurants.edit(id))
    }
  }
  
  def getPieChartData(id: Long): String = {
    val startTS = System.currentTimeMillis()
    val dishes = Dish.findAll(id)
    val activities = ActivityLog.findAllByType(365, 7)

    var str = ""
    for (dish <- dishes) {
      if (str.length() > 0) { str += ","}
      
      str += "['" + dish.name.replace(''',' ') + "', "
      var total = 0
      for (activity <- activities) {
        if (activity.activity_details.contains(dish.id + ",") || activity.activity_details.contains(dish.id + "]")) { 
          total += 1 
        }
      }
      str += total + "]"
    }
    Logger.info("getPieChartData elapsed: " + (System.currentTimeMillis() - startTS))
    str
  }

  val months = Array("January","February","March","April","May","June","July","August","September","October","November","December")
  
  def getBarChartData(id: Long): String = {
    getBarChartData(Dish.findAll(id))
  }
  
  def getBarChartData(dishes: Seq[Dish]): String = {
    
    val startTS = System.currentTimeMillis()
    val activities = ActivityLog.findAllByType(190, 7)
    var totals:Array[Int] = new Array[Int](13)

    for (dish <- dishes) {
      var total = 0
      for (activity <- activities) {
        if (activity.activity_details.contains(dish.id + ",") || activity.activity_details.contains(dish.id + "]")) { 
          val cal = Calendar.getInstance()
          cal.setTime(activity.createdate)
          val month = cal.get(Calendar.MONTH)
          totals(month) += 1
        }
      }
    }
    
    Logger.info("getBarChartData elapsed: " + (System.currentTimeMillis() - startTS))
    
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    
    var output = ""
    for (i <- (currentMonth + 6) to (currentMonth+12)) {
      if (output.length != 0) { output += "," }
      output += "['" + months(i%12) + "', " + totals(i%12) + "]"
    }
    
    output
  }
}
