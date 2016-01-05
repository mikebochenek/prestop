import akka.actor.{ Actor, Props }
import play.api.libs.concurrent.Akka
import play.api.GlobalSettings
import play.api.templates.Html
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.User
import actors.EmailJobActor
import java.util.Calendar
import java.util.Date
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.data._
import play.api.http.HeaderNames
import scala.concurrent.Future
import play.api.Logger
import play.api.mvc.Session

// http://stackoverflow.com/questions/23525022/play-2-2-for-scala-modifying-acceptlanguage-http-request-header-with-scalainte
object LangFromSubdomain extends Filter {
  def apply(next: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {

    val subdomainLanguage = request.headers.get(HeaderNames.ACCEPT_LANGUAGE).get /*.substring(0,2) match {
      case "it" => "it"
      case "es" => "es"
      case "de" => "de"
      case "fr" => "fr"
      case _ => "en"
    }*/

    val newHeaders = new Headers {
      val data = (request.headers.toMap
        + (HeaderNames.ACCEPT_LANGUAGE -> Seq(subdomainLanguage))).toList
    }

    val newRequestHeader = request.copy(headers = newHeaders)

    //Logger.info(request.headers.get(HeaderNames.ACCEPT_LANGUAGE) + "---> " + subdomainLanguage)

    next(newRequestHeader)
  }
}

/**
 * Application global object, used here to schedule jobs on application start-up.
 */
object Global extends WithFilters(LangFromSubdomain) {

  override def onStart(application: play.api.Application) {

    play.api.Logger.info("Scheduling jobs...")
    import scala.concurrent.duration._
    import play.api.Play.current

    val actor = Akka.system.actorOf(
      Props(new EmailJobActor()))

    Akka.system.scheduler.schedule(calculateDelayForSchedule.seconds, 1.days, actor, "send")
    //Akka.system.scheduler.schedule(0.seconds, 1.minutes, actor, "send")
  }

  /**
   * so hacky... can't believe that I have to do this
   * http://stackoverflow.com/questions/13700452/scheduling-a-task-at-a-fixed-time-of-the-day-with-akka
   * http://brainstep.blogspot.ch/2013/10/scheduling-jobs-in-play-2.html
   */
  private def calculateDelayForSchedule: Long = {
    var c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY, 10);
    c.set(Calendar.MINUTE, 22);
    c.set(Calendar.SECOND, 0);
    var plannedStart = c.getTime();
    val now = new Date();
    var nextRun = c.getTime();
    if (now.after(plannedStart)) {
      c.add(Calendar.DAY_OF_WEEK, 1);
      nextRun = c.getTime();
    }
    val delayInSeconds = (nextRun.getTime() - now.getTime()) / 1000; //To convert milliseconds to seconds.
    delayInSeconds
  }

  // 500 - internal server error
  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(
      views.html.error()))
  }

  // 404 - page not found error
  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.error()))
  }
}
