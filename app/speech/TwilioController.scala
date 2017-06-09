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

object TwilioController extends Controller with Secured {
  
  def hello() = Action {
    implicit request => {
      // Create a TwiML response and add our friendly message.
      val voiceResponse = new VoiceResponse.Builder()
                .say(new Say.Builder("Hello Monkey").build())
                .build();
      val xml = voiceResponse.toXml();
      Ok(xml).as("application/xml");
    }
  }
  
  def record() = Action {
    implicit request => {
      // Use <Say> to give the caller some instructions
      val instructions = new Say.Builder("Welcome to the Presto booking demo, powered by Google Speech. "
          + " Please leave a message after the beep.").build();

      // Use <Record> to record the caller's message
      val record = new Record.Builder().build();

      // End the call with <Hangup>
      val hangup = new Hangup();

      // Create a TwiML builder object
      val twiml = new VoiceResponse.Builder()
        .say(instructions)
        .record(record)
        .hangup(hangup)
        .build();

      val xml = twiml.toXml();
      Ok(xml).as("text/xml");
    }
  }
}