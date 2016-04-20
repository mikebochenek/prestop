package common

import sys.process._
import java.net.URL
import java.io.File

object FileDownloader {
  def download(url: String, filename: String) = {
    new URL(url) #> new File(filename) !!
  }
}