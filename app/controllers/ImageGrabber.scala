package controllers

import java.io.File
import play.api.libs.json._
import play.Play
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

object ImageGrabber extends Controller with Secured {

  def getFiles() = {
    val url = current.configuration.getString("image.server.localpath")
    val path = url.getOrElse("/home/mike/data/presto/") + "imagegrabber"
    val files = new java.io.File(path).listFiles.filter(_.getName.endsWith(".json")) 
    files
  }

  def load(name: String) = IsAuthenticated { username =>
    implicit request => {
      val allfiles = getFiles()
      val filenames = allfiles.map { x => x.getName }
      val selectedFile = filenames.find { x => name.equals(x) }.getOrElse(null)
      val file = allfiles.find { x => name.equals(x.getName) }.getOrElse(null)
      val buf = scala.collection.mutable.ArrayBuffer.empty[IGNode]

      if (name.length > 0) {
        val source = Source.fromFile(file).getLines

        
        while (source.hasNext) {
          val resp = IGResponse.getInstance(source.next)
        
          buf ++= resp.media.nodes
        }
        
      }

      Ok(views.html.imagegrabber(filenames, selectedFile, buf))
    }
  }
}
