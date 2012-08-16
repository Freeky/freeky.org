package bootstrap.liftweb

import java.sql.DriverManager
import net.liftweb._
import util._
import Helpers._
import common._
import http._
import sitemap._
import Loc._
import squerylrecord.SquerylRecord
import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web.model._
import com.jolbox.bonecp._
import java.io.FileOutputStream
import java.io.PrintWriter
import net.liftweb.util.Mailer._
import javax.mail.{ Authenticator, PasswordAuthentication }

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("de.freeky.web")

    // Build SiteMap
    val entries = List(
      Menu("home", S ? "home") / "index",
      Menu(S ? "blog") / "blog",
      Menu(S ? "projects") / "projects",
      Menu(S ? "about") / "about",
      Menu(S ? "register") / "register" >> Hidden >> If(() => !User.loggedIn_?(), S ? "no.permission"),
      Menu("options", S ? "options") / "options" >> Hidden >> If(() => User.loggedIn_?(), S ? "no.permission") >> LocGroup("main"),
      Menu(S ? "administration") / "administration" >> If(() => User.rights.hasAdminrights, S ? "no.permission") submenus (
        Menu(S ? "blog.list") / "blog" / "list" >> If(() => User.rights.rEditBlog, S ? "no.permission"),
        Menu(S ? "blog.create") / "blog" / "create" >> If(() => User.rights.rEditBlog, S ? "no.permission"),
        Menu(S ? "blog.edit") / "blog" / "edit" >> Hidden >> If(() => User.rights.rEditBlog, S ? "no.permission"),
        Menu(S ? "blog.delete") / "blog" / "delete" >> Hidden >> If(() => User.rights.rEditBlog, S ? "no.permission"),
        Menu("changemail", S ? "change.mail") / "options" / "changemail" >> Hidden >> If(() => User.loggedIn_?(), S ? "no.permission"),
        Menu("changepassword", S ? "change.password") / "options" / "changepassword" >> Hidden >> If(() => User.loggedIn_?(), S ? "no.permission"),
        Menu("deleteaccount", S ? "delete.account") / "options" / "deleteaccount" >> Hidden >> If(() => User.loggedIn_?(), S ? "no.permission"),
        Menu("administrate.users", S ? "administrate.users") / "administration" / "users" >> If(() => (User.rights.rAdministrateUsers), S ? "no.permission"),
        Menu("edit.projects", S ? "edit.projects") / "edit" / "project" >> If(() => (User.rights.rEditProjects), S ? "no.permission"),
        Menu("new.project", S ? "new.projects") / "new" / "project" >> If(() => (User.rights.rEditProjects), S ? "no.permission"),
        //Menu("send.mail", S ? "send.mail") / "administration" / "sendmail" >> If(() => (User.rights.rSendMail), S ? "no.permission"),
        Menu("list.staticpages", S ? "list.staticpages") / "administration" / "staticpage" / "list" >> If(() => (User.rights.rEditStaticPages), S ? "no.permission"),
        Menu("edit.staticpages", S ? "edit.staticpages") / "administration" / "staticpage" / "edit" >> Hidden >> If(() => (User.rights.rEditStaticPages), S ? "no.permission")),
      Menu("") / "css" / ** >> Hidden,
      Menu("") / "images" / ** >> Hidden,
      Menu("") / "js" / ** >> Hidden,
      Menu("") / "static" / ** >> Hidden)

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries: _*))

    appendRewrites
    LiftRules.statelessDispatchTable.append(de.freeky.web.lib.MySitemap)

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Set i18n support
    LiftRules.resourceNames = "i18n/fky" :: LiftRules.resourceNames

    // Define the loggedInTest
    LiftRules.loggedInTest = Full(() => loggedInUser.isDefined)

    // Defining BoneCP ConnectionPool
    Props.requireOrDie("db.url", "db.user", "db.password")
    Class.forName("com.mysql.jdbc.Driver")
    val boneCPconfig = new BoneCPConfig()
    boneCPconfig.setJdbcUrl(Props.get("db.url", ""))
    boneCPconfig.setUsername(Props.get("db.user", ""))
    boneCPconfig.setPassword(Props.get("db.password", ""))
    boneCPconfig.setMinConnectionsPerPartition(1)
    boneCPconfig.setMaxConnectionsPerPartition(5)
    val connectionPool = new BoneCP(boneCPconfig)

    // Defining Squeryl session management
    SquerylRecord.initWithSquerylSession(Session.create(
      connectionPool.getConnection(),
      new MySQLAdapter))

    // SQL zum erzeugen der Tabellen schreiben
    transaction {
      val sqlPrinter = new PrintWriter(new FileOutputStream("setup.sql"))
      FreekyDB.printDdl(sqlPrinter)
      sqlPrinter.flush()
      sqlPrinter.close()
    }

    setupMailer
  }

  def setupMailer = {
    Mailer.authenticator = for {
      user <- Props.get("mail.user")
      pass <- Props.get("mail.password")
    } yield new Authenticator {
      override def getPasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }
  }

  def appendRewrites = {
    LiftRules.statelessRewrite.append {
      case RewriteRequest(
        ParsePath(List("administration", "users", userId), _, _, _), _, _) =>
        RewriteResponse("administration" :: "users" :: Nil, Map("id" -> userId))
      case RewriteRequest(
        ParsePath(List("projects", project), _, _, _), _, _) =>
        RewriteResponse("projects" :: Nil, Map("project" -> project))
      case RewriteRequest(
        ParsePath(List("edit", "project", id), _, _, _), _, _) =>
        RewriteResponse("edit" :: "project" :: Nil, Map("id" -> id))
      case RewriteRequest(
        ParsePath(List("administration", "staticpage", "edit", id), _, _, _), _, _) =>
        RewriteResponse("administration" :: "staticpage" :: "edit" :: Nil, Map("id" -> id))
      case RewriteRequest(
        ParsePath(List("blog", "page", page), _, _, _), _, _) if (page.forall(_.isDigit)) =>
        RewriteResponse("blog" :: Nil, Map("page" -> page))
      case RewriteRequest(
        ParsePath(List("blog", "list", "page", page), _, _, _), _, _) if (page.forall(_.isDigit)) =>
        RewriteResponse("blog" :: "list" :: Nil, Map("page" -> page))
      case RewriteRequest(
        ParsePath(List("blog", "edit", id), _, _, _), _, _) if (id.forall(_.isDigit)) =>
        RewriteResponse("blog" :: "edit" :: Nil, Map("id" -> id))
      case RewriteRequest(
        ParsePath(List("blog", "delete", id), _, _, _), _, _) if (id.forall(_.isDigit)) =>
        RewriteResponse("blog" :: "delete" :: Nil, Map("id" -> id))
      case RewriteRequest(
        ParsePath(List("blog", id), _, _, _), _, _) if (id.forall(_.isDigit)) =>
        RewriteResponse("blog" :: Nil, Map("id" -> id))
    }
  }
}

