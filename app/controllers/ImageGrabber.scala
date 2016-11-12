package controllers

import java.io.File
import play.api.libs.json._
import play.Play
import play.api.Logger
import play.api.Play.current
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
import views._
import java.util.Date
import models.json.DishLikers
import java.nio.file.{Files, Path}
import scala.io.Source
import models.json.IGResponse
import models.json.IGNode
import common.FileDownloader

object ImageGrabber extends Controller with Secured {

  def createUrl(fullurl: String, name: String): String = {
    val urlConf = current.configuration.getString("image.server.baseurl")
    
    val url = fullurl.substring(0, fullurl.lastIndexOf('?'))
    val f = url.substring(url.lastIndexOf('/'))

    urlConf.getOrElse("http://localhost") + "imagegrabber/" + name.dropRight(5) + f  
  }
  
  def getPath() = {
    val url = current.configuration.getString("image.server.localpath")
    url.getOrElse("/home/mike/data/presto/") + "imagegrabber/"
  }
  
  def getFiles() = {
    new java.io.File(getPath).listFiles.filter(_.getName.endsWith(".json")) 
  }
  
  def getTagSuggestions() = {
    Tag.findAllPopular(Tag.TYPE_INGREDIENTS)
  }

  def load(name: String) = IsAuthenticated { username =>
    implicit request => {
      val allfiles = getFiles()
      val filenames = allfiles.map { x => x.getName }
      val selectedFile = filenames.find { x => name.equals(x) }.getOrElse(null)
      val file = allfiles.find { x => name.equals(x.getName) }.getOrElse(null)
      val buf = scala.collection.mutable.ArrayBuffer.empty[IGNode]

      val tags = getTagSuggestions

      if (name.length > 0) {
        val source = Source.fromFile(file).getLines
        
        while (source.hasNext) {
          val resp = IGResponse.getInstance(source.next)
        
          buf ++= resp.media.nodes
          
          val nodes = resp.media.nodes
          for (n <- nodes) {
            val fullurl = n.display_src
            val url = fullurl.substring(0, fullurl.lastIndexOf('?'))
            val f = url.substring(url.lastIndexOf('/'))
            
            val filename = getPath + name.dropRight(5) + f
            if (!(new java.io.File(filename).exists)) {
              Logger.debug ("ImageGrabber: " + filename + " downloading " + url)
              FileDownloader.download(url, filename)
            } else {
              Logger.debug ("ImageGrabber: " + filename + " already exists " + url)
            }
          }
        }
      }

      Ok(views.html.imagegrabber(filenames, selectedFile, buf.sortWith(_.likes.count > _.likes.count)))
    }
  }
}
