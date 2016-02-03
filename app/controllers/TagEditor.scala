package controllers

import java.io.File

import play.Play
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

object TagEditor extends Controller with Secured {

  def load() = IsAuthenticated { username =>
    implicit request => {
      Ok(views.html.taglist_edit(Tag.findAll))
    }
  }

  def loadTag(id: Long) = IsAuthenticated { username =>
    implicit request => {
      Logger.info("calling Tag edit - for id:" + id)
      val tag = Tag.findAll.filter { _.id == id }.head
      Ok(views.html.tag_edit(tagForm, tag, TagRef.findByTag(tag.id).size))
    }
  } 
  
  val tagForm = Form(
    tuple(
      "id" -> text,
      "name" -> text,
      "en_text" -> text,
      "de_text" -> text,
      "it_text" -> text,
      "fr_text" -> text))
      
  def save() = IsAuthenticated { username =>
    implicit request =>  
      val (id, name, en_text, de_text, it_text, fr_text) = tagForm.bindFromRequest.get
      Logger.info("calling tag update for id:" + id + " name:" + name)
      Tag.update(id.toInt, name, en_text, de_text, it_text, fr_text)
      Redirect(routes.TagEditor.loadTag(id.toLong))
  }  
}
