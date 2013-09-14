package de.freeky.web.snippet

import net.liftweb.http._
import net.liftweb.util._
import scala.xml._
import Helpers._
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web.model._
import de.freeky.web.lib.Security
import de.freeky.web.lib.AjaxFactory._
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

  def short = {
    if (User.rights.rAdministrateUsers) {
      
        ".overall" #> inTransaction {
          Text(from(FreekyDB.users)(u => compute(count)).toLong.toString) } &
          ".unconfirmed" #> inTransaction {
          Text(from(FreekyDB.users)(u => where(u.accounttypeId === Props.getLong("acc.new", 2)) compute (count)).toLong.toString)} &
            ".advanced" #> inTransaction {
          Text(from(FreekyDB.users, FreekyDB.accountTypes)((u, a) => where(u.accounttypeId === a.id and a.rAdmin === true) compute (count)).toLong.toString)}
    } else
      "*" #> ""
  }

  def overview(in: NodeSeq): NodeSeq = {
    var entrycount = 25
    var page = 0
    var filterType: Option[Long] = None
    var nameFilter: Option[String] = None

    var allocates: Option[List[Long]] =
      if (User.rights.rAdministrateUsers) None
      else transaction { Some(User.rights.allocates.map(_.id).toList) }

    val userEntryTemplate = (".entry ^^" #> ((n: NodeSeq) => n)).apply(in)

    def max = transaction { maxquery.toLong };

    def maxquery =
      from(FreekyDB.users)(u => where((u.accounttypeId === filterType.?) and
        (u.name like nameFilter.map("%%%s%%".format(_)).?) and {
          allocates.map(a => (u.accounttypeId in a)).getOrElse(1 === 1)
        }) compute (count))
    def query =
      from(FreekyDB.users)(u => where((u.accounttypeId === filterType.?) and
        (u.name like nameFilter.map("%%%s%%".format(_)).?) and {
          allocates.map(a => (u.accounttypeId in a)).getOrElse(1 === 1)
        }) select (u) orderBy (u.id))

    def buildUserTable(entries: List[User]) = {
      entries.flatMap({ entry =>
        (".id" #> Text(entry.id.toString) &
          ".name" #> Text(entry.name) &
          ".type" #> Text(entry.accountType.head.name) &
          ".editlink [href]" #> "/administration/users/%d".format(entry.id)).apply(userEntryTemplate)
      })
    }

    def userTable() = {
      transaction {
        val entries = query.page(page * entrycount, entrycount).toList
        buildUserTable(entries)
      }
    }

    def updateUserTable(): JsCmd = {
      List(JsCmds.SetHtml("user_table", userTable),
        JsCmds.SetHtml("current_page", Text("%d/%d".format(page + 1, (max / entrycount) + 1))))
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
      if (((page + 1) * entrycount) < max) page = page + 1
      updateUserTable
    }

    def updateFilterType(t: String) = {
      filterType = tryo { t.toLong }
      updateUserTable
    }

    def updateNameFilter(n: String) = {
      nameFilter = if (n.length() > 0) Some(n) else None
      updateUserTable
    }

    (".entrycount" #> SHtml.ajaxSelect(List(10, 25, 50, 100).map(i => (i.toString, i.toString)),
      Full(25.toString), v => updateEntryCount(v)) &
      ".page" #> Text("%d/%d".format(page + 1, (max / entrycount) + 1)) &
      ".prevpage" #> SHtml.ajaxButton(Text(S ? "previous"), () => prevPage) &
      ".nextpage" #> SHtml.ajaxButton(Text(S ? "next"), () => nextPage) &
      ".typefilter" #> SHtml.ajaxSelect(
        ("a", "All") :: transaction { FreekyDB.accountTypes.map(at => (at.id.toString, at.name)).toList } ::: Nil,
        Full(filterType.map(_.toString).getOrElse("a")), updateFilterType(_)) &
        ".namefilter" #> ajaxLiveText(nameFilter.getOrElse(""), updateNameFilter(_)) &
        ".entry" #> userTable).apply(in)
  }

  def editUser = {
    var password = ""
    var passwordretype = ""
    var accountType = ""
    val userOption = S.param("id") match {
      case Full(idStr) => {
        transaction {
          if (User.rights().rAdministrateUsers)
            from(FreekyDB.users)(u => where(u.id === idStr.toLong) select (u)).headOption
          else
            from(FreekyDB.users)(u =>
              where(u.accounttypeId in User.rights.allocates.map(_.id)
                and u.id === idStr.toLong) select (u)).headOption
        }
      }
      case _ => None
    }

    def buildAccountTypeValues(): List[(String, String)] = {
      transaction {
        if (User.rights.rAdministrateUsers)
          FreekyDB.accountTypes.map(at => (at.id.toString, at.name)).toList
        else
          User.rights.allocates.map(at => (at.id.toString, at.name)).toList
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
              if (User.rights.rAdministrateUsers ||
                User.rights().allocates.map(at => at.id).toList.contains(newAT))
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
        ".name" #> SHtml.text(user.name, user.name = _) &
          ".password" #> SHtml.password(password, password = _) &
          ".passwordretype" #> SHtml.password(passwordretype, passwordretype = _) &
          ".email" #> SHtml.text(user.email, user.email = _) &
          ".registrationdate" #> Text(user.registrationdate.toString()) &
          ".accounttype" #> SHtml.select(buildAccountTypeValues, Full(user.accounttypeId.toString), accountType = _) &
          ".submit" #> SHtml.submit(S ? "edit", processEditUser)
      }
      case _ => "*" #> ""
    }
  }
}