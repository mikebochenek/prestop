name := "bitesapp"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2"

libraryDependencies += "net.tanesha.recaptcha4j" % "recaptcha4j" % "0.0.7"

libraryDependencies += "com.stripe" % "stripe-java" % "2.7.0"

libraryDependencies += "com.google.cloud" % "google-cloud-speech" % "0.17.1-alpha"

libraryDependencies += "com.twilio.sdk" % "twilio-java-sdk" % "7.0.0-rc-7"

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.8.0"

play.Project.playScalaSettings
