package de.freeky.web.model

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema

object FreekyDB extends Schema {

  val users = table[User]
  val accountTypes = table[AccountType]
  val loginAttempts = table[LoginAttempt]
  val projects = table[Project]
  val staticPages = table[StaticPage]
  val blogs = table[Blog]

  val accountTypeToUser =
    oneToManyRelation(accountTypes, users).via((a, u) => a.id === u.accounttypeId)

  val userToLoginAttempt =
    oneToManyRelation(users, loginAttempts).via((u, l) => u.id === l.userId)

  val userToBlogs =
    oneToManyRelation(users, blogs).via((u, b) => u.id === b.authorId)

  on(users)(u => declare(
    u.name is (unique, indexed, dbType("varchar(20)")),
    u.email is (unique, indexed, dbType("varchar(128)")),
    u.passwordhash is (dbType("varchar(30)")),
    u.passwordsalt is (dbType("varchar(20)"))))

  on(accountTypes)(a => declare(
    a.name is (dbType("varchar(20)")),
    a.description is (dbType("varchar(1000)"))))

  on(loginAttempts)(l => declare(
    l.userId is (indexed),
    l.time is (indexed),
    l.ip is (dbType("varchar(39)"))))

  on(projects)(p => declare(
    p.description is (dbType("varchar(256)")),
    p.text is (dbType("text"))))

  on(staticPages)(sp => declare(
    sp.name is (unique, indexed, dbType("varchar(64)")),
    sp.content is (dbType("text"))))

  on(blogs)(b => declare(
    b.title is (dbType("varchar(256)")),
    b.text is (dbType("text"))))
}






