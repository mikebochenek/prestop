package speech

/*
 * https://raw.githubusercontent.com/GoogleCloudPlatform/java-docs-samples/master/speech/cloud-client/src/main/java/com/example/speech/QuickstartSample.java
 */
// [START speech_quickstart]
// Imports the Google Cloud client library
import com.google.cloud.speech.spi.v1.SpeechClient;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

object Quickstart {
  def main(args:Array[String] ):Unit = {
    //System.out.println(process("/tmp/recording.wav"))
    if (args.length > 0) {
      System.out.println(process(args(0)))
    } else {
      System.out.println(process("/tmp/speech-1497024871640.wav"))
    }
  }
  
  def process(fileName: String) = {
    // Instantiates a client
    val speech = SpeechClient.create()
    
    // Reads the audio file into memory
    val path = Paths.get(fileName);
    val data = Files.readAllBytes(path);
    val audioBytes = ByteString.copyFrom(data);

    // Builds the sync recognize request
    val config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        //.setSampleRateHertz(16000)
        .setLanguageCode("en-US")
        .build();
    val audio = RecognitionAudio.newBuilder()
        .setContent(audioBytes)
        .build();

    // Performs speech recognition on the audio file
    val response = speech.recognize(config, audio)
    val results = response.getResultsList().toArray // List<SpeechRecognitionResult>

    var output = "";
    for (result <- results) {
      val alternatives = result.asInstanceOf[SpeechRecognitionResult].getAlternativesList().toArray // List<SpeechRecognitionAlternative> 

      for (alternative <- alternatives) {
        val transcript = alternative.asInstanceOf[SpeechRecognitionAlternative]
        output += transcript.getTranscript();
        val confidence = transcript.getConfidence();
      }
    }
    
    speech.close();
    output;
  }
}
// [END speech_quickstart]