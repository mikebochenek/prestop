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

play.Project.playScalaSettings
