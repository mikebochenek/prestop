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
    val fileName = "./resources/audio.raw";
    
  }
/*
    // Instantiates a client
    SpeechClient speech = SpeechClient.create();

    // The path to the audio file to transcribe
    String fileName = "./resources/audio.raw";

    // Reads the audio file into memory
    Path path = Paths.get(fileName);
    byte[] data = Files.readAllBytes(path);
    ByteString audioBytes = ByteString.copyFrom(data);

    // Builds the sync recognize request
    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setSampleRateHertz(16000)
        .setLanguageCode("en-US")
        .build();
    RecognitionAudio audio = RecognitionAudio.newBuilder()
        .setContent(audioBytes)
        .build();

    // Performs speech recognition on the audio file
    RecognizeResponse response = speech.recognize(config, audio);
    List<SpeechRecognitionResult> results = response.getResultsList();

    for (SpeechRecognitionResult result: results) {
      List<SpeechRecognitionAlternative> alternatives = result.getAlternativesList();
      for (SpeechRecognitionAlternative alternative: alternatives) {
        System.out.printf("Transcription: %s%n", alternative.getTranscript());
      }
    }
    speech.close();
  }
  * 
  */
}
// [END speech_quickstart]