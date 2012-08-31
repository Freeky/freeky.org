package de.freeky.web.snippet

import _root_.scala.xml.{ NodeSeq, Text }
import _root_.net.liftweb.util.Helpers
import _root_.net.liftweb.http._
import _root_.net.liftweb.common._
import Helpers._
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web.model._
import S._
import net.liftweb.textile._
import net.liftweb.http.js.JsCmds
import java.sql.Timestamp
import de.freeky.web.lib.AjaxFactory._

class Static extends DispatchSnippet {

  def dispatch: DispatchIt = _ match {
    case "show" => show
    case "list" => list
    case "generate" => generate
    case "edit" => edit
  }

  def show = {
    val site = S.attr("site") openOr ""

    transaction {
      from(FreekyDB.staticPages)(sp => where(sp.name === site) select (sp)).headOption
    } match {
      case Some(site) =>
        "*" #>
          <div>
            <head>
              <title>{ site.title }</title>
              <meta name="description" content={ site.description }/>
              <meta name="keywords" content={ site.keywords }/>
            </head>
            { TextileParser.paraFixer(TextileParser.toHtml(site.content)) }
          </div>
      case _ => {
        generatePages
        S.redirectTo("/")
      }
    }
  }

  def list = {
    transaction {
      "*" #> from(FreekyDB.staticPages)(sp => select(sp)).map(page =>
        ".id" #> Text(page.id.toString) &
          ".name" #> Text(page.name) &
          ".editlink [href]" #> "/administration/staticpage/edit/%d".format(page.id))
    }
  }

  def edit = {
    val pageId = (S.param("id") openOr "0").toLong
    val dbPage = transaction { FreekyDB.staticPages.lookup(pageId) }

    def processEdit() = {
      dbPage.map(page => {
        page.lastModified = new Timestamp(millis)
        transaction { FreekyDB.staticPages.update(page) }
      })
    }

    def updateShowContent(text: String) = {
      dbPage match {
        case Some(page) =>
          page.content = text
          JsCmds.SetHtml("showcontent", TextileParser.toHtml(text))
        case _ => S.redirectTo("/administration/staticpage/list")
      }
    }

    dbPage match {
      case Some(page) =>
        ".name" #> Text(page.name) &
          ".editcontent" #> ajaxLiveTextarea(page.content, updateShowContent(_)) &
          ".edittitle" #> SHtml.text(page.title, page.title = _) &
          ".editdescription" #> SHtml.text(page.description, page.description = _) &
          ".editkeywords" #> SHtml.text(page.keywords, page.keywords = _) &
          "#showcontent *" #> TextileParser.paraFixer(TextileParser.toHtml(page.content)) &
          ".submit" #> SHtml.submit(S ? "edit", processEdit)
      case _ => S.redirectTo("/administration/staticpage/list")
    }
  }

  def generate = {
    ".submit" #> SHtml.submit(S ? "generate.static.pages", generatePages)
  }

  def generatePages() = {
    transaction {
      FreekyDB.staticPages.insertOrUpdate(new StaticPage(1, "index", "", new Timestamp(millis), "", "", ""))
      FreekyDB.staticPages.insertOrUpdate(new StaticPage(2, "about", "", new Timestamp(millis), "", "", ""))
      FreekyDB.staticPages.insertOrUpdate(new StaticPage(3, "contact", "", new Timestamp(millis), "", "", ""))
      FreekyDB.staticPages.insertOrUpdate(new StaticPage(4, "impressum", "", new Timestamp(millis), "", "", ""))
    }
  }
}