package common

import models.UserFull
import models.Recommendations
import models.Restaurant
import models.Dish
import models.Image
import models.Tag
import play.api.Logger


object Recommendation {
  //47.385740, 8.518084 coordinates for Zurich Hardbrucke
  //47.411875, 8.548024 Zurich Oerlikon Neudorfstrasse 23 
  //47.356842, 8.514578 Zurich Uetlibergstrasse 231
  //46.953082, 7.446915 Bern
  
  val maxdist = 10 //TODO this should be more like .8
  def recommend(user: UserFull, longitude: Double, latitude: Double) = {
    val restaurants = Map(Restaurant.findAll map { a => a.id -> a}: _*)
    // http://stackoverflow.com/questions/2925041/how-to-convert-a-seqa-to-a-mapint-a-using-a-value-of-a-as-the-key-in-the-ma
    
    //TODO we can not iterate in a dumb for-loop because this would not scale
    //TODO ideally, we would read the restaurants only once in a while..
    
    val priceMin = 0
    val priceMax = 40.0
    
    val dishes = Dish.findAll()
      .filter { x => within(maxdist, restaurants, x.restaurant_id, longitude, latitude) }
      .filter { x => (priceMax >= x.price && priceMin <= x.price) }    
    
    for (dish <- dishes) {
      dish.url = Image.findByDish(dish.id).headOption.getOrElse(Image.blankImage).asInstanceOf[Image].url
      dish.distance = Haversine.haversine(restaurants.get(dish.restaurant_id).head.latitude, 
          restaurants.get(dish.restaurant_id).head.longitude, latitude, longitude)
      dish.tags = Tag.findByRef(dish.id, 11).map(_.name)
    }
    val r = new Recommendations(dishes);
    r
  }
  
  def within(max: Double, restaurants: Map[Long, Restaurant], id: Long, longitude: Double, latitude: Double) = {
    // http://www.cis.upenn.edu/~matuszek/cis554-2011/Pages/scalas-option-type.html
    restaurants.get(id) match {
      case Some(f) =>
        val distance = Haversine.haversine(f.latitude, f.longitude, latitude, longitude)
        Logger.debug("distance: " + distance + " inputs:" + (f.latitude + " " + f.longitude + " " + longitude + " " + latitude))
        distance < max
      case None => false
    }
  }  
}