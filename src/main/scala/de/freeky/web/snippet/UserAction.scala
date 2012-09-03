package de.freeky.web.snippet

import net.liftweb.http._
import net.liftweb.util._
import net.liftweb.proto._
import scala.xml._
import Helpers._
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web.model._
import de.freeky.web.lib.Security
import net.liftweb.common._
import java.sql.Timestamp

class UserAction extends StatefulSnippet with Logger {
  def dispatch = _ match {
    case "register" => register
    case "login" => login
    case "logout" => logout
    case "status" => status
    case "changepassword" => changePassword
    case "changemail" => changeEmail
  }

  val nameRegex = "[a-zA-Z1-9]+".r.pattern

  var username = ""
  var email = ""
  var password = ""
  var passwordretype = ""

  def register(in: NodeSeq): NodeSeq = {

    def processRegister(): Unit = {
      var errors = checkRegistrationInformations

      // No Errors
      if (errors.isEmpty) {
        val salt = Security.generateSalt()
        val hash = Security.hashPassword(password, salt)

        var user = new User
        user.name = username
        user.email = email
        user.passwordhash = hash
        user.passwordsalt = salt
        user.accounttypeId = Props.getLong("acc.new", 0)

        transaction {
          user = FreekyDB.users.insert(user)
        }

        info("New user registered (mame: %s, email: %s)".format(user.name, user.email))

        this.unregisterThisSnippet()

        S.redirectTo("index")

        // Process Errors
      } else {
        errors.foreach(S.error(_))
      }
    }

    loggedInUser.is match {
      case Full(user) => Text("")
      case _ => bind("register", in,
        "name" -> SHtml.text(username, username = _),
        "email" -> SHtml.text(email, email = _),
        "password" -> SHtml.password("", password = _),
        "passwordretype" -> SHtml.password("", passwordretype = _),
        "submit" -> SHtml.submit(S ? "sign.up", processRegister))
    }
  }

  def checkRegistrationInformations: List[String] = {
    var errors = List[String]()

    errors = checkNewPassword(password, passwordretype) ::: errors

    if (!nameRegex.matcher(username).matches) {
      errors = (S ? "username.contains.invalid.chars") :: errors
    }

    if (username.length > 20) {
      errors = (S ? "username.too.long") :: errors
    }

    transaction {
      val sameName = from(FreekyDB.users)(u =>
        where(u.name === username) select (u))

      val sameEmail = from(FreekyDB.users)(u =>
        where(u.email === email) select (u))

      if (!sameName.isEmpty) {
        errors = (S ? "username.already.taken") :: errors
      }

      if (!sameEmail.isEmpty) {
        errors = (S ? "email.already.in.use") :: errors
        info("Try for Multiaccount on Email: " + sameEmail.head.email)
      }
    }
    errors
  }

  def checkNewPassword(pw: String, pwr: String): List[String] = {
    var errors = List[String]()

    if (!pw.equals(pwr)) {
      errors = (S ? "passwords.do.not.match") :: errors
    }
    if (!(pw.length() >= 6)) {
      errors = (S ? "password.too.short") :: errors
    }

    errors
  }

  def login = {

    def processLogin(): Unit = {
      transaction {
        from(FreekyDB.users)(u =>
          where(u.email === email) select (u)).headOption match {
          case Some(user) => {

            val previsousAttempts = user.loginAttempts.where(l =>
              l.time > new Timestamp(millis - 10.minutes)
                and l.success === false
                and l.validated === false).size

            if (previsousAttempts >= Props.getInt("acc.login.attempts", 3)) {
              S.error(S ? "too.many.false.attempts")
              return
            }

            val loginAttempt = new LoginAttempt(user.id,
              S.request match {
                case Full(req) => req.remoteAddr
                case _ => "unknown"
              })

            val hash = Security.hashPassword(password, user.passwordsalt)

            if (hash equals user.passwordhash) {
              val rLogin = user.accountType.headOption match {
                case Some(a) => a.rLogin
                case _ => false
              }

              if (!rLogin) {
                S.error(S ? "account.not.active")
                info("Try to login into inactive account with ID: " + user.id)
              } else {
                loggedInUser(Full(user))
              }
            } else {

              loginAttempt.success = false
              info("Invalid password entered for account with ID: " + user.id)
              S.error(S ? "incorrect.user.or.password")
            }

            FreekyDB.loginAttempts.insert(loginAttempt)

          }
          case _ => S.error(S ? "incorrect.user.or.password")
        }
      }

    }

    loggedInUser.is match {
      case Full(user) => "*" #> ""
      case _ => {
          ".email" #> SHtml.text(email, email = _) &
          ".password" #> SHtml.password("", password = _) &
          ".submit" #> SHtml.submit(S ? "login", processLogin)
      }
    }
  }

  def processLogout(): Unit = {
    loggedInUser(Empty)
  }

  def logout(in: NodeSeq): NodeSeq = {
    loggedInUser.is match {
      case Full(user) => SHtml.link("index", processLogout, Text(S ? "logout"))
      case _ => Text("")
    }
  }

  def status = {
    loggedInUser.is match {
      case Full(user) => 
        ".username" #> user.name &
        ".logout" #> SHtml.submit(S ? "logout", processLogout)
      case _ => "*" #> ""
    }
  }

  def changePassword(in: NodeSeq): NodeSeq = {
    var currentPassword = ""
    var newPassword = ""
    var repeatNewPassword = ""

    def processChangePassword = {
      var errors = List[String]()
      loggedInUser.is match {
        case Full(user) => {
          if (!Security.hashPassword(currentPassword, user.passwordsalt).equals(user.passwordhash)) {
            errors = (S ? "passwords.do.not.match") :: errors
          }
          errors = checkNewPassword(newPassword, repeatNewPassword) ::: errors

          if (errors.isEmpty) {
            user.passwordsalt = Security.generateSalt()
            user.passwordhash = Security.hashPassword(newPassword, user.passwordsalt)

            transaction {
              FreekyDB.users.update(user)
            }
            S.notice(S ? "new.password.set")
            S.redirectTo("/options")
          } else {
            errors.foreach(S.error(_))
          }
        }
        case _ => S.error(S ? "no.permission")
      }
    }
    loggedInUser.is match {
      case Full(user) => {
        bind("changepassword", in,
          "currentpassword" -> SHtml.password(currentPassword, currentPassword = _),
          "newpassword" -> SHtml.password(newPassword, newPassword = _),
          "newpasswordretype" -> SHtml.password(repeatNewPassword, repeatNewPassword = _),
          "submit" -> SHtml.submit(S ? "change", () => processChangePassword))
      }
      case _ => {
        warn("changePassword-snippet was invoked but no user was set")
        Text("There was an error")
      }
    }
  }

  def changeEmail(in: NodeSeq): NodeSeq = {
    def processChangeEmail = {
      loggedInUser.is match {
        case Full(user) => {
          user.email = email
          transaction {
            FreekyDB.users.update(user)
          }

          S.notice(S ? "new.email.set")
          S.redirectTo("/options")
        }
        case _ => S.error(S ? "no.permission")
      }
    }

    loggedInUser.is match {
      case Full(user) => {
        bind("changemail", in,
          "email" -> SHtml.text(email, email = _),
          "submit" -> SHtml.submit(S ? "change", () => processChangeEmail))
      }
      case _ => {
        warn("changeEmail-snippet was invoked but no user was set")
        Text("There was an error")
      }
    }
  }
}