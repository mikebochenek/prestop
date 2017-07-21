package speech

import play.Play
import play.api.mvc.Action
import play.api.mvc.Session
import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import play.api.Play.current
import models._

import com.twilio.twiml._
import controllers.Secured
import common.FileDownloader
import actors.EmailJobActor
import common.EmailReport
import com.twilio.twiml.Say.Voice
import controllers.Reservations
import controllers.Settings
import play.api.cache.Cache
import java.util.Calendar
import java.text.SimpleDateFormat
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object TwilioController extends Controller with Secured {

  val timeoutSeconds = 3
  
  val initialPrompt = ("Hello!  Welcome to Alice Choo!  Unfortunately we can not come to the phone right now. "
          + " Our automated assistant can help you make the reservation. "
          + " What date and time is the reservation for?")
  val pleaseRepeat = new Say.Builder("I'm sorry, I did not understand.  Can you please repeat?").voice(Voice.ALICE).build(); 
  
  def errorNoFreeTables() = {
    createFirstPromptWithString("There is no availability at this time.  Please try again.  What date and time is the reservation for?")
  }

  def errorClosed() = {
    createFirstPromptWithString("We are closed at this time.  Please try again.  What date and time is the reservation for?")
  }

  def error() = {
    createFirstPromptWithString("I did not understand.  Please try again.  What date and time is the reservation for?")
  }
  
  def successful(p: String) = {
    val hangup = new Hangup(); // End the call with <Hangup>
    val instructions = new Say.Builder(p).voice(Voice.ALICE).build()
    new VoiceResponse.Builder().say(instructions).hangup(hangup).build()
  }
  
  def createFirstPrompt() = {
    createFirstPromptWithString(initialPrompt)
  }

  def createFirstPromptWithString(p: String) = { 
    val instructions = new Say.Builder(p).voice(Voice.ALICE).build(); // Use <Say> to give the caller some instructions
    val record = new Record.Builder().action("https://presto.bochenek.ch/api/twilio/record")
        .playBeep(false).timeout(timeoutSeconds).build(); // Use <Record> to record the caller's message
    val twiml = new VoiceResponse.Builder() // Create a TwiML builder object
        .say(instructions)
        .record(record)
        .say(pleaseRepeat)
        .build();
    twiml
  }

  def record() = Action {
    implicit request => {
      Ok(createFirstPrompt.toXml()).as("text/xml");
    }
  }

  def createFinalPrompt() = {
    val instructions = new Say.Builder("How many people in your party?").voice(Voice.ALICE).build();
    val record = new Record.Builder().action("https://presto.bochenek.ch/api/twilio/record2")
        .playBeep(false).timeout(timeoutSeconds).build();
    val twiml = new VoiceResponse.Builder()
        .say(instructions)
        .record(record)
        .say(pleaseRepeat)
        .build();
    twiml
  }
  
  val sleepMS = 3000
  def handleRecording() = Action {
    implicit request => {
      Logger.info("HTTP post to /api/record: " + request.body.asFormUrlEncoded)
      
      val from = request.body.asFormUrlEncoded.get("From")
      
      future = Future {
        Thread.sleep(sleepMS)
        val transcript = transcribeURL(request.body.asFormUrlEncoded.get("RecordingUrl"))
        Logger.info("transcript: " + transcript)
        Cache.set("recording1" + from(0), transcript)
        val extracted = SUTime.extract(transcript, suDefaultDateFormat.format(Calendar.getInstance.getTime))
        extracted
      }

      Ok(createFinalPrompt.toXml()).as("text/xml");
    }
  }
  
  var future: Future[String] = null

  def handleFinalRecording() = Action {
    implicit request => {
      Thread.sleep(sleepMS)
      Logger.info("HTTP post to /api/record2: " + request.body.asFormUrlEncoded)
      
      val transcript = transcribeURL(request.body.asFormUrlEncoded.get("RecordingUrl"))
      Logger.info("transcript: " + transcript)

      val from = request.body.asFormUrlEncoded.get("From")
      val called = request.body.asFormUrlEncoded.get("Called")
      
      //based on number being dialed, fetch restaurant, extract text, and create booking
      val restaurantID = identifyRestaurant(called) 
      //based on caller phone number, fetch or create a user
      val userID = identifyOrCreateUser(from)
      Logger.info("called: " + called + " from: " + from + " restaurantID: " 
          + restaurantID + " userID: " + userID)

      val time = Await.result(future, 1 nano)
      Logger.info("result from future thread: " + time)
      Logger.info("old extractTime(" + Cache.get("recording1" + from(0)))
      val guestCount = extractGuestCount(transcript)
      val comments = "" //TODO

      val reservationsID = (Reservations.parseTime(time) match {
        case null => Reservations.ERROR
        case _    => Reservations.makeReservation(restaurantID, userID, time, guestCount, comments)
      })
      Logger.info("Reservations.makeReservation: " + reservationsID)
      
      if (reservationsID == Reservations.NO_FREE_TABLES) {
         Ok(errorNoFreeTables.toXml()).as("text/xml");
      } else  if (reservationsID == Reservations.NOT_OPEN) {
         Ok(errorClosed.toXml()).as("text/xml");
      } else  if (reservationsID == Reservations.ERROR) {
         Ok(error.toXml()).as("text/xml");
      } else {
         EmailReport.sendtranscript(Cache.get("recording1" + from(0)).getOrElse("") 
             + "<br>" + transcript
             + "<br>" + "parsed time: " + time
             + "<br>" + "parsed guest count: " + guestCount
             + "<br>" + "restaurantID: " + restaurantID
             + "<br>" + "userID (based on phone): " + userID
             + "<br>" + "reservationsID: " + reservationsID
             , from.head)

         Ok(successful("OK.  Reservation created successfully for " 
             + guestCount + " guests, on " + responseFormat.format(Reservations.parseTime(time)) 
             + ".  Your reservation ID is " + reservationsID).toXml()).as("text/xml"); //final response should be using Alice's voice
      }
    }
  }
  def responseFormat = new SimpleDateFormat("EEEEEEEEEEEEEEE, MMMMMMMMM d, 'at' K:mm a")

  def transcribeURL(url: Seq[String]) = {
    val startTS = System.currentTimeMillis
    val filename = "/tmp/speech-" + System.currentTimeMillis + ".wav" 
      
    Logger.info("downloading " + url + " to " + filename)
      
    if (url.size > 0 ) {
      FileDownloader.download(url.head, filename)
        
      val transcript = Quickstart.process(filename)
      Logger.info("Quickstart transcript: " + transcript + "    " + (System.currentTimeMillis()-startTS) + "ms")
      
      transcript    
    } else {
      Logger.info("no wav/sound url")
      ""
    }
  }
  
  def identifyRestaurant(called: Seq[String]) = {
    //TODO I wounder if I should check restuarant status (to disallow bookings for deleted restaurants!)
    var found = -1L
    if (called.size > 0 && called(0) != null) {
      val all = RestaurantSeating.findAll()
      for (seating <- all) { //TODO this should be a do-while not found loop (because duplicate phone numbers create issues
        val misc = Reservations.getPreviousMiscSafely(seating)
        if (misc.reservationsPhone.getOrElse("").trim.equals(called(0).trim)) {
          found = seating.restaurant_id
        }
      }
    }
    found
  }
  
  def identifyOrCreateUser(from: Seq[String]) = {
    var found = -1L
    if (from.size > 0 && from(0) != null) {
      val userByPhone = User.getFullUserByPhone(Settings.cleanPhoneString(from(0)))
      
      if (userByPhone.size > 0) {
        found = userByPhone(0).id
      } else {
        //TODO could not identifyUserBy phone, we should create a place holder user
      }
    }
    found
  }
  
  val suDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")// 2017-07-20T16:00
  val suDefaultDateFormat = new SimpleDateFormat("yyyy-MM-dd")
  def extractTime(tstring: Option[Any]) = {
    val startTS = System.currentTimeMillis
    val t = tstring.getOrElse("2017-07-09T12:08:56.235-0700").toString //TODO
    Logger.info("tstring: " + tstring)
    val extracted = SUTime.extract(t, suDefaultDateFormat.format(Calendar.getInstance.getTime))
    Logger.info("extracted: " + extracted + "    " + (System.currentTimeMillis()-startTS) + "ms")
    extracted
  }
  
  def extractGuestCount(gc: String) = { //TODO hackathon-mode - needs better logic at some point
    val c = try {
      gc.toLong
    } catch {
      case ex: NumberFormatException => { 2 }
    } finally {
      2
    }
    c
  }
  
  
  /** native Twilio transcription service implementation */
  
  def recordNative() = Action {
    implicit request => {
      Ok(createNativePrompt.toXml()).as("text/xml");
    }
  }

  def createNativePrompt() = {
    val instructions = new Say.Builder("Let's try native transcription quality and speed.  What date and time is the reservation for?").voice(Voice.ALICE).build();
    val record = new Record.Builder().transcribe(true)
       .transcribeCallback("https://presto.bochenek.ch/api/twilio/handleTrans")
       .action("https://presto.bochenek.ch/api/twilio/recordNative").timeout(timeoutSeconds).build();
    val twiml = new VoiceResponse.Builder()
        .say(instructions)
        .record(record)
        .say(pleaseRepeat)
        .build();
    twiml
  }
  
  def handleRecordingNative() = Action {
    implicit request => {
      Logger.info("HTTP post to /api/recordNative: " + request.body.asFormUrlEncoded)
      Ok(successful("Thank you").toXml()).as("text/xml")
    }
  }
  
  def handleNativeTrans() = Action {
    implicit request => {
      Logger.info("HTTP post to /api/handleTrans: " + request.body.asFormUrlEncoded)
  
      val from = request.body.asFormUrlEncoded.get("From")
      
      future = Future {
        Thread.sleep(sleepMS)
        val transcript = request.body.asFormUrlEncoded.get("TranscriptionText")
        Logger.info("native transcript: " + transcript)
        EmailReport.sendTwilioTranscript(transcript.head, from.head)
        val extracted = SUTime.extract(transcript.head, suDefaultDateFormat.format(Calendar.getInstance.getTime))
        Logger.info("native extracted: " + extracted)
        extracted
      }
      
      Ok("OK")
    }
  }


}