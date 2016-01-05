import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "eventual"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "mysql" % "mysql-connector-java" % "5.1.21",
    "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
    "com.thoughtworks.xstream" % "xstream" % "1.4.7",
    "net.sf.opencsv" % "opencsv" % "2.3")

  val main = play.Project(appName, appVersion, appDependencies).settings()
}