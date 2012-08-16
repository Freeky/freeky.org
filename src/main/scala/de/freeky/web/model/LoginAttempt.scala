package de.freeky.web.model

import net.liftweb.util.Helpers._
import org.squeryl.KeyedEntity
import org.squeryl.dsl.ManyToOne
import java.sql.Timestamp

class LoginAttempt(val id: Long,
    val userId: Long,
    val time: Timestamp,
    var success: Boolean,
    val ip: String,
    var validated: Boolean) extends KeyedEntity[Long] {
  
  def this(user: Long, ip: String) = this(0, user, new Timestamp(millis), true, ip, false)
  
  lazy val user: ManyToOne[User] = FreekyDB.userToLoginAttempt.right(this)
}