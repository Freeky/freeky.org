package de.freeky.web.snippet

import java.text.SimpleDateFormat
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Table
import de.freeky.web.lib.AjaxFactory.ajaxLiveTextarea
import de.freeky.web.model._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds
import net.liftweb.http._
import net.liftweb.textile.TextileParser
import net.liftweb.util._
import Helpers._
import java.sql.Timestamp

/**
 * List of generic Urls:
 * - "%s" 				= first page
 * - "%s/%d" 			= specific article
 * - "%s/page/%d" 		= page of articles
 * - "%s/list" 			= first page of list of articles
 * - "%s/list/page/%d" 	= page of list of articles
 * - "%s/edit/%d" 		= edit specific article
 * - "%s/delete/%d" 	= delete specific article
 */
trait ArticleSnippet[U <: Article] extends DispatchSnippet {

  val baseUrl: String // Without trailing Slash (Example: /article)
  val autoPublish: Boolean

  val articles: Table[U]

  val factory: ArticleFactory[U]

  def dispatch: DispatchIt = _ match {
    case "show" => show
    case "showlatest" => showLatest
    case "list" => list
    case "new" => create
    case "create" => create
    case "delete" => delete
    case "edit" => edit
    case "render" => show
  }

  /**
   * show Method
   * returns Article
   * @param in
   * @return
   */
  def show = {

    val id = S.param("id").openOr("0").toLong
    val page = S.param("page").openOr("1").toInt
    val pagesize = S.attr("pagesize").openOr("5").toInt
    val entries = transaction { articles.where(a => a.published <= Some(new Timestamp(millis))).Count.toLong }

    var article: List[U] =
      transaction {
        if (id > 0) {
          articles.lookup(id).toList
        } else {
          from(articles)(a => where(a.published <= Some(new Timestamp(millis))) select (a) orderBy (a.created desc)).page((page - 1) * pagesize, pagesize).toList
        }
      }

    // make prev page link if required
    def prev =
      if (id > 0 || page <= 1)
        ("*" #> "")
      else
        "* [href]" #> ("%s/page/%d" format (baseUrl, page - 1))

    // make next page link if required
    def next =
      if (id > 0 || maxpages(entries, pagesize) <= page)
        ("*" #> "")
      else
        "* [href]" #> ("%s/page/%d" format (baseUrl, page + 1))

    // processes the given article entries to real HTML-entries 
    def bindArticle = article.map(a =>
      ".title *" #> a.title &
        ".text *" #> TextileParser.paraFixer(TextileParser.toHtml(a.text)) &
        ".date" #> timestamp.format(a.published.getOrElse(now)) &
        ".author" #> transaction { a.author.headOption.map(_.name).getOrElse("unknown") } &
        ".id" #> a.id &
        ".link [href]" #> ("%s/%d".format(baseUrl, a.id)))

    ".entry" #> bindArticle &
      ".previsious" #> prev &
      ".next" #> next
  }

  def showLatest = {
    transaction {
      articles.where(a => a.published <= Some(new Timestamp(millis))).lastOption.map(a =>
        ".title *" #> a.title &
          ".text *" #> TextileParser.paraFixer(TextileParser.toHtml(a.text)) &
          ".date" #> timestamp.format(a.published.getOrElse(now)) &
          ".author" #> a.author.headOption.map(_.name).getOrElse("unknown") &
          ".id" #> a.id &
          ".link [href]" #> ("%s/%d".format(baseUrl, a.id))).getOrElse("*" #> "")
    }
  }

  def list = {
    val page = S.param("page").openOr("1").toInt
    val pagesize = S.attr("pagesize").openOr("50").toInt
    val entries = transaction { articles.Count.toLong }

    val article: List[U] =
      transaction {
        from(articles)(a =>
          select(a) orderBy (a.created desc)).page((page - 1) * pagesize, pagesize).toList
      }

    // make prev page link if required
    def prev =
      if (page <= 1) ("*" #> "")
      else "* [href]" #> "%s/list/page/%d".format(baseUrl, page - 1)

    // make next page link if required
    def next =
      if (maxpages(entries, pagesize) <= page) ("*" #> "")
      else "* [href]" #> "%s/list/page/%d".format(baseUrl, page + 1)

    // processes the given article entries to real HTML-entries 
    def bind = article.map(a =>
      ".title" #> a.title &
        ".date" #> timestamp.format(a.created) &
        ".author" #> transaction { a.author.headOption.map(_.name).getOrElse("unknown") } &
        ".id" #> a.id &
        ".published" #> { if (a.published.isDefined) { S ? "yes" } else { S ? "no" } } &
        ".editlink [href]" #> "%s/edit/%d".format(baseUrl, a.id) &
        ".deletelink [href]" #> "%s/delete/%d".format(baseUrl, a.id))

    ".entry" #> bind &
      ".previsious" #> prev &
      ".next" #> next
  }

  /**
   * maxpages Method
   * calculates max amount of pages for given entries,
   * why there could be max $pagesize entries per page
   * @param entries
   * @param pagesize
   * @return
   */
  def maxpages(entries: Long, pagesize: Int): Long =
    if (entries % pagesize > 0)
      (entries / pagesize) + 1
    else
      entries / pagesize

  def timestamp = new SimpleDateFormat("dd MMMMM, yyyy", java.util.Locale.ENGLISH)

  /**
   * createNews Method
   * @param in
   * @return
   */
  def create = {
    val article = factory.create
    article.authorId = loggedInUser.map(_.id).openOr(0L)

    def addNewsToDatabase() = {
      if (article.title.eq("") && article.text.eq(""))
        S.error(S.?("fill.title.and.text"))
      else {
        transaction {
          articles.insert(article)
        }
        S.redirectTo(baseUrl)
      }
    }

    def preview = TextileParser.toHtml(article.text)

    def updatePreview(text: String): JsCmd = {
      article.text = text
      JsCmds.SetHtml("previewarea", preview)
    }

    ".title" #> SHtml.text(article.title, article.title = _) &
      ".text" #> ajaxLiveTextarea(article.text, updatePreview) &
      ".author" #> transaction { article.author.headOption.map(_.name).getOrElse("unknown") } &
      ".published" #> SHtml.checkbox(autoPublish, p => if (p) { article.published = Some(new Timestamp(millis)) } else article.published = None) &
      ".submit" #> SHtml.submit(S ? "add", addNewsToDatabase) &
      "#previewarea *" #> preview
  }

  def delete = {
    val id = S.param("id").openOr("0").toLong

    val article = transaction { articles.lookup(id).headOption }

    if (article.isEmpty)
      S.redirectTo("/")

    def deleteArticleFormDatabase() = {
      transaction { article.map(a => articles.deleteWhere(qa => qa.id === a.id)) }
      S.redirectTo("%s/list".format(baseUrl))
    }

    article.map(a =>
      ".title" #> a.title &
        ".text" #> TextileParser.paraFixer(TextileParser.toHtml(a.text)) &
        ".date" #> timestamp.format(a.published.getOrElse(now)) &
        ".author" #> transaction { a.author.headOption.map(_.name).getOrElse("unknown") } &
        ".id" #> a.id &
        ".submit" #> SHtml.submit(S ? "delete", deleteArticleFormDatabase)).getOrElse("*" #> (S ? "entry.not.found"))
  }

  def edit = {
    val id = S.param("id").openOr("0").toLong

    val article = transaction { articles.lookup(id) }

    if (article.isEmpty)
      S.redirectTo("/")

    article.map(_.authorId = loggedInUser.map(_.id).openOr(0L))

    def updateArticleInDatabase() = {
      article.map(a =>
        if (a.title.eq("") && a.text.eq(""))
          S.error(S.?("fill.title.and.text"))
        else {
          transaction { articles.update(a) }
          S.redirectTo(baseUrl)
        })
    }

    def preview = TextileParser.toHtml(article.map(_.text).getOrElse(""))

    def updatePreview(text: String): JsCmd = {
      article.map(_.text = text)
      JsCmds.SetHtml("previewarea", preview)
    }

    article.map(a =>
      ".title" #> SHtml.text(a.title, a.title = _) &
        ".text" #> ajaxLiveTextarea(a.text, updatePreview) &
        ".author" #> transaction { a.author.headOption.map(_.name).getOrElse("unknown") } &
        ".published" #> SHtml.checkbox(a.published.isDefined, p =>
          if (p && a.published.isEmpty) {
            a.published = Some(new Timestamp(millis))
          } else if (!p && a.published.isDefined)
            a.published = None) &
        ".submit" #> SHtml.submit(S ? "edit", updateArticleInDatabase) &
        "#previewarea *" #> preview).getOrElse("*" #> (S ? "entry.not.found"))
  }
}