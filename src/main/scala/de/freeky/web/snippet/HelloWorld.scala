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
import net.liftweb.http.js._
import JE._
import JsCmds._
import JsExp._

class HelloWorld extends StatefulSnippet {
  
  registerThisSnippet()

  def dispatch = _ match {
    case "render" => create
  }

  val salt = Safe.randomString(16)
  var name = ""

  var nodeSeq: NodeSeq = NodeSeq.Empty

  def doSomething() = S.notice("send2")
  
  def create = 
    ".button1" #> SHtml.submit("Drück mich",() => S.notice("send"), "onclick" -> 
    	JsIf(JsRaw("confirm('Sind Sie sicher?')"), 
      JsReturn(true), JsReturn(false)).toJsCmd
    ) &
    ".button2" #> SHtml.submit("Drück mich auch", doSomething)

}

