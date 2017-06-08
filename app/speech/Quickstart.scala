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
    // Instantiates a client
    val speech = SpeechClient.create()
    
    // The path to the audio file to transcribe
    val fileName = "/home/mike/Downloads-1/audio.raw"; // brooklyn.flac ? 

    // Reads the audio file into memory
    val path = Paths.get(fileName);
    val data = Files.readAllBytes(path);
    val audioBytes = ByteString.copyFrom(data);

    // Builds the sync recognize request
    val config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setSampleRateHertz(16000)
        .setLanguageCode("en-US")
        .build();
    val audio = RecognitionAudio.newBuilder()
        .setContent(audioBytes)
        .build();

    // Performs speech recognition on the audio file
    val response = speech.recognize(config, audio)
    val results = response.getResultsList().toArray // List<SpeechRecognitionResult>

    for (result <- results) {
      val alternatives = result.asInstanceOf[SpeechRecognitionResult].getAlternativesList().toArray // List<SpeechRecognitionAlternative> 

      for (alternative <- alternatives) {
        val transcript = alternative.asInstanceOf[SpeechRecognitionAlternative]
        System.out.printf("Transcription: %s%n", transcript);
      }
    }
    
    speech.close();
  }
}
// [END speech_quickstart]