package de.freeky.web.snippet

import scala.xml.{ NodeSeq, Text }
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import java.util.Date
import de.freeky.web.lib._
import Helpers._
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web.model._
import S._

class HelloWorld extends StatefulSnippet {
  
  registerThisSnippet()

  def dispatch = _ match {
    case "render" => create
  }

  val salt = Safe.randomString(16)
  var name = ""

  var nodeSeq: NodeSeq = NodeSeq.Empty

  def create = 
    ".subnode" #> { (n: NodeSeq) => nodeSeq = n; n } &
      ".node" #> nodeSeq &
      ".node2" #> nodeSeq

}

