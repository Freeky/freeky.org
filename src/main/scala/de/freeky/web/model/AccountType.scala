package de.freeky.web.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl.OneToMany

// Rechte mit prefix r
// default false

class AccountType(val id: Long,
    var name: String,
    var description: Option[String],
    var rLogin: Boolean,
    var rAdministrateUsers: Boolean,
    var rEditProjects: Boolean,
    var rSendMail: Boolean,
    var rEditStaticPages: Boolean,
    var rEditBlog: Boolean,
    var rUseForum: Boolean,
    var rAdministrateForums: Boolean,
    var rManageImages: Boolean) extends KeyedEntity[Long] {
  
  def this() = this(0, "", None, false, false, false, false, false, false, false, false, false)
  
  def rAdmin: Boolean = {
    rAdministrateUsers ||
    rAdministrateForums
  }
  
  lazy val users: OneToMany[User] = FreekyDB.accountTypeToUser.left(this)
  lazy val allocators = FreekyDB.accountTypeAssignations.right(this)
  lazy val allocates = FreekyDB.accountTypeAssignations.left(this)
  
  def hasAdminrights = rAdministrateUsers || rEditBlog || rEditProjects || rEditStaticPages
}