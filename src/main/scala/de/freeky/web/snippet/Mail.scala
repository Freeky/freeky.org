package de.freeky.web.snippet

import scala.xml.{ NodeSeq, Text }
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util.Mailer._
import java.util.Date
import de.freeky.web.lib._
import Helpers._
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web.model._
import S._

class Mail extends StatefulSnippet {

  def dispatch = _ match {
    case "sendform" => sendForm
  }

  def sendForm = {

    var subject = ""
    var text = <html xmlns="http://www.w3.org/1999/xhtml"
	xmlns:lift="http://liftweb.net/">
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
</head>
<body>
	<div style="width: 15cm;">
		<div id="header" class="column span-23 last">

			<span class="lift:Menu.item?linkToSelf=true&name=home;"> <img
				src="http://www.gods-of-tramp.de/images/logo.png" alt="Trampolin München" />
			</span>
		</div>

		<div class="column span-23 last toparea">
			<div id="content">
				<!-- The HTML content will go here -->
				Some Content
			</div>
		</div>
		<div id="copyright" class="span-23" style="text-align: center;">
			<h4 class="alt">
				Copyrights 2011-2012<br />by Daniel Millet <span
					class="lift:Menu.item?linkToSelf=true&name=impressum;">Impressum</span>
				 <span class="lift:Menu.item?linkToSelf=true&name=agb;">AGB</span>
			</h4>
		</div>
	</div>
</body>
</html>
    var to = ""

    def mail() = {
      Mailer.sendMail(
        From("noreply@freeky.org"),
        Subject(subject),
        To(to),
        text)
    }

    ".to" #> SHtml.text(to, to = _) &
      ".subject" #> SHtml.text(subject, subject = _) &
      //".text" #> SHtml.textarea(text, text = _) &
      ".submit" #> SHtml.submit(S ? "send", mail)
  }

}