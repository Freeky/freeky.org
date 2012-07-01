package de.freeky.web.snippet

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util._
import scala.xml._
import Helpers._

import de.freeky.web.model._

class Options extends DispatchSnippet with Logger {
  def dispatch() = _ match {
    case "show" => show
  }

  def show(in: NodeSeq): NodeSeq = {
    loggedInUser.is match {
      case Full(user) => {
        bind("show", in,
          "registrationdate" -> Text(user.registrationdate.toString()),
          "mail" -> Text(user.email),
          "changemail" -> <lift:Menu.item name="changemail"/>,
          "changepassword" -> <lift:Menu.item name="changepassword"/>,
          "deleteaccount" -> <lift:Menu.item name="deleteaccount"/>)
      }
      case _ => {
        warn("show-snippet was invoked but no user was set")
        Text("There was an error")
      }
    }
  }

}