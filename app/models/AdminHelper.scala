package models

import play.api.Logger
import scala.sys.process._

object AdminHelper {

  val mybr = "<hr style=\"width: 100%; color: black; height: 1px; background-color:black;\" />"
  
  def generateStats(): String = {

    (system_date + system_uptime + mybr
        + system_vnstat + mybr
        + "image storage size: " + image_storage_size + mybr
        + "user count:" + User.countAll + "<br>" 
        + "restaurant count:" + Restaurant.countAll + "<br>" 
        + "friend count:" + Friend.countAll + "<br>" 
        + "reservations:" + Reservation.countAll + "<br>" 
        + "activitylog count:" + ActivityLog.countAll + "<br>" 
        + "tag ref count:" + TagRef.findAll.size + "<br>" 
        + "<br>" + mybr
        + system_df + "<br><br>" + mybr + system_top + "<br><br>").replaceAll("\n", "<br>")
  }

  //http://stackoverflow.com/questions/16162483/execute-external-command
  def system_df(): String = { "df".!! }

  def system_top(): String = { "top -b -n1 -o %MEM".!! }
  
  def system_date(): String = { "date".!! }

  def system_uptime(): String = { "uptime".!! }
  
  def image_storage_size(): String = { "du -sh /home/mike/data/presto".!! }

  def system_vnstat(): String = { "vnstat".!! }
}