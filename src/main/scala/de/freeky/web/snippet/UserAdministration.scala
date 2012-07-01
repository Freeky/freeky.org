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
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmd

class UserAdministration extends StatefulSnippet {
  def dispatch = _ match {
    case "short" => short
    case "overview" => overview
    case "edituser" => editUser
  }

  def short(in: NodeSeq): NodeSeq = {
    if (User.rights.rAdministrateUsers) {
      transaction {
        bind("short", in,
          "overall" -> Text(
            from(FreekyDB.users)(u => compute(count)).toLong.toString),
          "unconfirmed" -> Text(
            from(FreekyDB.users)(u => where(u.accounttypeId === Props.getLong("acc.new", 3)) compute (count)).toLong.toString))
      }
    } else
      Text("")
  }

  def overview(in: NodeSeq): NodeSeq = {
    var entrycount = 25
    var page = 0

    def buildUserTable(entries: List[User], template: NodeSeq) = {
      entries.flatMap({ entry =>
        bind("entry", chooseTemplate("overview", "entry", template),
          "id" -> Text(entry.id.toString),
          "name" -> Text(entry.name),
          "type" -> Text(entry.accountType.head.name),
          "editlink" -> { nodes: NodeSeq =>
            <a href={ "/administration/users/" + entry.id.toString }>{ nodes }</a>
          })
      })
    }

    def userTable() = {
      transaction {
        val entries =
          from(FreekyDB.users)(u => select(u)).page(page * entrycount, entrycount)
        buildUserTable(entries.toList, in)
      }
    }

    def updateUserTable(): JsCmd = {
      List(JsCmds.SetHtml("user_table", userTable),
        JsCmds.SetHtml("current_page", Text((page + 1).toString)))
    }

    def updateEntryCount(e: String) = {
      entrycount = Integer.parseInt(e)
      page = 0
      updateUserTable
    }

    def prevPage = {
      if (page > 0) page = page - 1
      updateUserTable
    }

    def nextPage = {
      transaction {
        val max = if (!User.rights().rAdministrateUsers)
          from(FreekyDB.users)(u => where(u.accounttypeId === Props.getLong("acc.default", 2)) select (u)).count(u => true)
        else
          from(FreekyDB.users)(u => select(u)).count(u => true)
        if (((page + 1) * entrycount) < max) page = page + 1
      }
      updateUserTable
    }

    bind("overview", in,
      "entrycount" -> SHtml.ajaxSelect(List(10, 25, 50, 100).map(i => (i.toString, i.toString)),
        Full(25.toString), v => updateEntryCount(v)),
      "page" -> Text((page + 1).toString),
      "prevpage" -> SHtml.ajaxButton(Text(S ? "previous"), () => prevPage),
      "nextpage" -> SHtml.ajaxButton(Text(S ? "next"), () => nextPage),
      "table" -> userTable)
  }

  def editUser(in: NodeSeq): NodeSeq = {
    var password = ""
    var passwordretype = ""
    var accountType = ""
    val userOption = S.param("id") match {
      case Full(idStr) => {
        transaction {
          from(FreekyDB.users)(u => where(u.id === idStr.toLong) select (u)).headOption
        }
      }
      case _ => None
    }

    def buildAccountTypeValues(): List[(String, String)] = {
      transaction {
        FreekyDB.accountTypes.map(at => (at.id.toString, at.name)).toList
      }
    }

    def processEditUser() = {
      userOption match {
        case Some(user) => {
          transaction {
            // new password
            if (password.length > 0 || passwordretype.length > 0) {
              if (password.equals(passwordretype)) {
                user.passwordsalt = Security.generateSalt()
                user.passwordhash = Security.hashPassword(password, user.passwordsalt)
              } else {
                S.error(S ? "passwords.do.not.match")
              }
            }

            // new accounttype
            if (user.accounttypeId != accountType.toLong) {
              val newAT = accountType.toLong
              if (FreekyDB.accountTypes.map(at => at.id).toList.contains(newAT))
                user.accounttypeId = newAT
              else
                S.error(S ? "not.allowed")
            }

            FreekyDB.users.update(user)
          }
        }
        case _ => ()
      }
    }

    userOption match {
      case Some(user) => {
        bind("user", in,
          "name" -> SHtml.text(user.name, user.name = _),
          "password" -> SHtml.password(password, password = _),
          "passwordretype" -> SHtml.password(passwordretype, passwordretype = _),
          "email" -> SHtml.text(user.email, user.email = _),
          "registrationdate" -> Text(user.registrationdate.toString()),
          "accounttype" -> SHtml.select(buildAccountTypeValues, Full(user.accounttypeId.toString), accountType = _),
          "submit" -> SHtml.submit(S ? "edit", processEditUser))
      }
      case _ => Text("")
    }
  }
}