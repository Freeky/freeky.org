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
  val images = table[Image]
  val forums = table[Forum]
  val forumTopics = table[ForumTopic]
  val forumPosts = table[ForumPost]

  val accountTypeAssignations =
    manyToManyRelation(accountTypes, accountTypes).via[AccountTypeAssignation]((a1, a2, aa) => (aa.allocator === a1.id, aa.allocated === a2.id))
    
  val accountTypeToUser =
    oneToManyRelation(accountTypes, users).via((a, u) => a.id === u.accounttypeId)

  val userToLoginAttempt =
    oneToManyRelation(users, loginAttempts).via((u, l) => u.id === l.userId)

  val userToBlogs =
    oneToManyRelation(users, blogs).via((u, b) => u.id === b.authorId)
    
  val userToImages =
    oneToManyRelation(users, images).via((u,i) => u.id === i.uploaderId)

  val forumPostToForumLastPost =
    oneToManyRelation(forumPosts, forums).via((fp, f) => fp.id === f.last_post_id)

  val forumToParents =
    oneToManyRelation(forums, forums).via((f1, f2) => f1.id === f2.parentid)

  val forumToForumTopics =
    oneToManyRelation(forums, forumTopics).via((f, ft) => f.id === ft.forumid)

  val userToForumTopics =
    oneToManyRelation(users, forumTopics).via((u, ft) => u.id === ft.userid)

  val forumTopicToForumPosts =
    oneToManyRelation(forumTopics, forumPosts).via((ft, fp) => ft.id === fp.topicid)

  val forumToForumPosts =
    oneToManyRelation(forums, forumPosts).via((f, fp) => f.id === fp.forumid)

  val userToForumPosts =
    oneToManyRelation(users, forumPosts).via((u, fp) => u.id === fp.userid)

  val userToForumPostEditUser =
    oneToManyRelation(users, forumPosts).via((u, fp) => u.id === fp.edit_user_id)

  val forumsToModerators =
    manyToManyRelation(forums, users).via[ForumModerator]((f, u, fm) => (fm.forumId === f.id, fm.userId === u.id))

  val forumsReadAccessToAccountType =
    manyToManyRelation(forums, accountTypes).via[ForumReadAccess]((f, at, fra) => (fra.forumId === f.id, fra.accountTypeId === at.id))

  val forumsWriteAccessToAccountType =
    manyToManyRelation(forums, accountTypes).via[ForumWriteAccess]((f, at, fwa) => (fwa.forumId === f.id, fwa.accountTypeId === at.id))

    
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
    sp.content is (dbType("text")),
    sp.description is (dbType("varchar(256)")),
    sp.keywords is (dbType("varchar(512)")),
    sp.title is (dbType("varchar(128)"))))

  on(blogs)(b => declare(
    b.title is (dbType("varchar(256)")),
    b.text is (dbType("text"))))

  on(images)(i => declare(
    i.name is (dbType("varchar(256)")),
    i.secure is (unique, indexed, dbType("varchar(16)")),
    i.mimeType is (dbType("varchar(64)"))))
  
  on(accountTypeAssignations)(a => declare(
    a.allocator is (indexed)))
    
  on(forums)(f => declare(
    f.name is (dbType("varchar(256)")),
    f.description is (dbType("varchar(1024)"))))

  on(forumTopics)(ft => declare(
    ft.title is (dbType("varchar(256)"))))

  on(forumPosts)(fp => declare(
    fp.text is (dbType("text")),
    fp.subject is (dbType("varchar(256)"))))

  on(forumsToModerators)(ftm => declare(
    ftm.forumId is (indexed)))

  on(forumsReadAccessToAccountType)(fratat => declare(
    fratat.forumId is (indexed)))

  on(forumsWriteAccessToAccountType)(fwatat => declare(
    fwatat.forumId is (indexed)))
}