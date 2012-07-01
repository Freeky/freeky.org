package de.freeky.web.model

import net.liftweb.http.SessionVar
import net.liftweb.common._
import net.liftweb.util.{Props, Helpers}
import Helpers._

import org.squeryl.KeyedEntity
import org.squeryl.dsl._
import org.squeryl.PrimitiveTypeMode._

import java.sql.Timestamp
import de.freeky.web.lib.Security

object loggedInUser extends SessionVar[Box[User]](Empty)

class User(val id: Long,
		var name: String,
		var email: String,
		var passwordhash: String,
		var passwordsalt: String,
		var accounttypeId: Long,
		val registrationdate: Timestamp) extends KeyedEntity[Long] {
  
  // import props -> do calendar etc
  def this() = this(0, "", "", "", Security.generateSalt, Props.getLong("acc.new", 0),new Timestamp(millis))
  
  lazy val accountType: ManyToOne[AccountType] = FreekyDB.accountTypeToUser.right(this)
  lazy val loginAttempts: OneToMany[LoginAttempt] = FreekyDB.userToLoginAttempt.left(this)
}

object User {
  def loggedIn_?() = loggedInUser.isDefined
  
  def rights(): AccountType = loggedInUser.is match {
    case Full(user) => transaction {
      user.accountType.head
    }
    case _ => new AccountType
  }
}