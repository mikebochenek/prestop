package models

import play.api.Logger
import scala.sys.process._

object AdminHelper {

  val mybr = "<hr style=\"width: 100%; color: black; height: 1px; background-color:black;\" />"
  
  def deletedImages(): String = {
    val di = Image.findAll().filter { x => x.status == -1 }
    di.map{_.filename}.mkString("<br>") + "<br><br>deleted: " + di.size
  }
  
  def generateStats(): String = {

    (system_date + system_uptime + mybr
        + system_vnstat + mybr
        + "image storage size: " + image_storage_size + mybr
        + "user count:" + User.countAll + "<br>" 
        + "restaurant count:" + Restaurant.countAll + "<br>" 
        + "dish count:" + Dish.countAll + "<br>" 
        + "friend count:" + Friend.countAll + "<br>" 
        + "reservations:" + Reservation.countAll + "<br>" 
        + "activitylog count:" + ActivityLog.countAll + "<br>" 
        + "tag ref count:" + TagRef.findAll.size + "<br>" 
        + "<br>" + mybr
        + system_df + "<br><br>" + mybr 
        + system_top.split("\n").filter { x => x.contains("%MEM") || x.contains("java") }.mkString("\n") 
        + "<br><br>" + mybr + system_free + "<br><br>" + mybr 
        + (java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / (1000 * 60 * 60)) 
        + " hours JVM uptime."  + "<br><br>" + mybr
        + "last " + system_gitlog
     ).replaceAll("\n", "<br>")
  }

  //http://stackoverflow.com/questions/16162483/execute-external-command
  def system_df(): String = { "df".!! }

  def system_free(): String = { "free".!! }

  def system_top(): String = { "top -b -n1".!! }
  
  def system_date(): String = { "date".!! }

  def system_uptime(): String = { "uptime".!! }
  
  def image_storage_size(): String = { "du -sh /home/mike/data/presto".!! }

  def system_vnstat(): String = { "vnstat".!! }
  
  def system_gitlog(): String = { "git log -1".!! }
}
