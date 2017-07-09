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
import models._

import com.twilio.twiml._
import controllers.Secured
import common.FileDownloader
import actors.EmailJobActor
import common.EmailReport
import com.twilio.twiml.Say.Voice
import controllers.Reservations
import controllers.Settings

object TwilioController extends Controller with Secured {

  val timeoutSeconds = 3
  
  val initialPrompt = ("Unfortunately we can not come to the phone right now. "
          + " Our automated assistant can help you make the reservation. "
          + " What day is the reservation for?")
  val pleaseRepeat = new Say.Builder("I'm sorry, I did not understand.  Can you please repeat?").voice(Voice.ALICE).build(); 
  
  def errorNoFreeTables() = {
    createFirstPromptWithString("There is no availability at this time.  Please try again.  What day is the reservation for?")
  }

  def errorClosed() = {
    createFirstPromptWithString("We are closed at this time.  Please try again.  What day is the reservation for?")
  }

  def error() = {
    createFirstPromptWithString("I did not understand.  Please try again.  What day is the reservation for?")
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
    val record = new Record.Builder().timeout(timeoutSeconds).build(); // Use <Record> to record the caller's message
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
    val record = new Record.Builder().action("https://presto.bochenek.ch/api/twilio/record2").timeout(timeoutSeconds).build();
    val twiml = new VoiceResponse.Builder()
        .say(instructions)
        .record(record)
        .say(pleaseRepeat)
        .build();
    twiml
  }
  
  val sleepMS = 5000
  def handleRecording() = Action {
    implicit request => {
      Thread.sleep(sleepMS)
      Logger.info("HTTP post to /api/record: " + request.body.asFormUrlEncoded)
      
      val from = request.body.asFormUrlEncoded.get("From")
      
      val transcript = transcribeURL(request.body.asFormUrlEncoded.get("RecordingUrl"))
      Logger.info("transcript: " + transcript)
        
      //EmailReport.sendtranscript(transcript, from.head)
        
      Ok(createFinalPrompt.toXml()).as("text/xml");
    }
  }


  def handleFinalRecording() = Action {
    implicit request => {
      Thread.sleep(sleepMS)
      Logger.info("HTTP post to /api/record2: " + request.body.asFormUrlEncoded)
      
      val transcript = transcribeURL(request.body.asFormUrlEncoded.get("RecordingUrl"))
      Logger.info("transcript: " + transcript)

      val from = request.body.asFormUrlEncoded.get("From")
      val called = request.body.asFormUrlEncoded.get("Called")
      
      val restaurantID = identifyRestaurant(called) 
      val userID = identifyOrCreateUser(from)
      Logger.info("called: " + called + " from: " + from + " restaurantID: " 
          + restaurantID + " userID: " + userID)
      
      //TODO based on caller phone number, fetch or create a user
      //TODO based on number being dialed, fetch restaurant, extract text, and create booking
      
      val time = "2017-07-09T12:08:56.235-0700" //TODO
      val guestCount = extractGuestCount(transcript)
      val comments = "" //TODO

      val reservationsID = Reservations.makeReservation(restaurantID, userID, time, guestCount, comments)
      Logger.info("Reservations.makeReservation: " + reservationsID)
      
      if (reservationsID == Reservations.NO_FREE_TABLES) {
         Ok(errorNoFreeTables.toXml()).as("text/xml");
      } else  if (reservationsID == Reservations.NOT_OPEN) {
         Ok(errorClosed.toXml()).as("text/xml");
      } else  if (reservationsID == Reservations.ERROR) {
         Ok(error.toXml()).as("text/xml");
      } else {
         Ok(successful("OK.  Reservation created." + reservationsID).toXml()).as("text/xml"); //final response should be using Alice's voice
      }

      
    }
  }

  def transcribeURL(url: Seq[String]) = {
    val filename = "/tmp/speech-" + System.currentTimeMillis + ".wav" 
      
    Logger.info("downloading " + url + " to " + filename)
      
    if (url.size > 0 ) {
      FileDownloader.download(url.head, filename)
        
      val transcript = Quickstart.process(filename)
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
  
  def extractGuestCount(gc: String) = { //TODO hackathon-mode - needs better logic at some point
    if ("one".equals(gc.toLowerCase)) 1
    else if ("two".equals(gc.toLowerCase)) 2 
    else if ("three".equals(gc.toLowerCase)) 3 
    else if ("four".equals(gc.toLowerCase)) 4 
    else if ("five".equals(gc.toLowerCase)) 5 
    else if ("six".equals(gc.toLowerCase)) 6 
    else if ("seven".equals(gc.toLowerCase)) 7 
    else if ("eight".equals(gc.toLowerCase)) 8 
    else if ("nine".equals(gc.toLowerCase)) 9 
    else if ("ten".equals(gc.toLowerCase)) 10 
    2
  }
}