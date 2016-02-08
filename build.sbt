name := "bitesapp"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

libraryDependencies += "org.imgscalr" % "imgscalr-lib" % "4.2"

play.Project.playScalaSettings
