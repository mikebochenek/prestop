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

object TwilioController extends Controller with Secured {

  val timeoutSeconds = 3
  
  val initialPrompt = ("Unfortunately we can not come to the phone right now. "
          + " Our automated assistant can help you make the reservation. "
          + " What day is the reservation for?")
  val pleaseRepeat = new Say.Builder("I'm sorry, I did not understand.  Can you please repeat?").voice(Voice.ALICE).build(); 
          
  def createFirstPrompt() = {
    val instructions = new Say.Builder(initialPrompt).voice(Voice.ALICE).build(); // Use <Say> to give the caller some instructions
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
    val hangup = new Hangup(); // End the call with <Hangup>
    val twiml = new VoiceResponse.Builder()
        .say(instructions)
        .record(record)
        .hangup(hangup)
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
      
      Logger.info("called: " + called + " from: " + from)
      
      //TODO based on caller phone number, fetch or create a user
      //TODO based on number being dialed, fetch restaurant, extract text, and create booking
      
      Ok("OK"); //TODO final response should be using Alice's voice
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
      Logger.info("no image url")
      ""
    }
  }
}