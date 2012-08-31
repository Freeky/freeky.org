package de.freeky.web.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl._
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import net.liftweb.util.Helpers._
import net.liftweb.util.Props
import scala.xml.Node
import java.io.File

class Image(val id: Long,
  var name: String,
  var uploaderId: Long,
  var mimeType: String,
  var secure: String,
  val uploaded: Timestamp) extends KeyedEntity[Long] {

  def this() = this(0L, "", 0L, "", "", new Timestamp(millis))

  lazy val uploader = FreekyDB.userToImages.right(this)

  def link = "/image/%s/%s".format(secure, name)
  def detailLink = "/image/detail/%d" format id
  def path = "%s/%s".format(Props.get("imagepath", "./images"), id)

  def file = {
    val path = new File(Props.get("imagepath", "./images"))
    val file = new File(path, id.toString)
    file
  }
  def toHTML(width: Int, height: Int): Node =
    <img src={ "/image/%s/%s?width=%d&height=%d".format(secure, name, width, height) }>{ name }</img>

  def toHTML: Node =
    <img src={ "/image/%s/%s".format(secure, name) }>{ name }</img>;
}