package de.freeky.web.snippet

import net.liftweb.common._
import scala.xml.{ NodeSeq, Text }
import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.proto.ProtoRules
import Helpers._
import de.freeky.web.model._
import S._
import java.text.SimpleDateFormat
import org.squeryl.PrimitiveTypeMode._



class MySitemapContent {

  case class Entry(date: java.util.Date, url: String)
  
  lazy val lastModFormat = new SimpleDateFormat()
  lastModFormat.applyPattern("yyyy-MM-dd")
  
  lazy val entries: Iterable[Entry] = static ++ blogs
  lazy val static: Iterable[Entry] = transaction{from(FreekyDB.staticPages)(sp => select(sp)).map(page => Entry(page.lastModified, "/%s".format(page.name)))}
  lazy val blogs: Iterable[Entry] = transaction{from(FreekyDB.blogs)(b => select(b)).map(blog => Entry(blog.modified, "/blog/%s".format(blog.id)))}
  //lazy val news = Entry(News.findAll(OrderBy(News.editDate, Descending)).head.editDate.is, "/news")
  //lazy val pictures = Entry(Image.findAll(OrderBy(Image.uploadDate, Descending)).head.uploadDate.is, "/pictures")
  
  def base: CssSel =
    "loc *" #> "http://%s/".format(S.hostName) &
    "lastmod *" #> lastModFormat.format(Helpers.now)

  def list: CssSel =
    "url *" #> entries.map(post =>
      "loc *" #> "http://%s%s".format(S.hostName, post.url) &
      "lastmod *" #>  lastModFormat.format(post.date))

}