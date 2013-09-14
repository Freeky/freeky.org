package de.freeky.web.model
import org.squeryl.KeyedEntity
import org.squeryl.dsl._
import java.sql.Timestamp

class Forum(val id: Long,
    var parentid: Option[Long],
    var name: String,
    var description: Option[String],
    var last_post_id: Option[Long],
    var ordering: Int) extends KeyedEntity[Long] {
  
  def this() = this(0, Some(0L), "", Some(""), Some(0L), 10000)
  def this(name: String) = this(0, None, name, None, None, 10000)
  
  lazy val parent: ManyToOne[Forum] = FreekyDB.forumToParents.right(this)
  lazy val childs: OneToMany[Forum] = FreekyDB.forumToParents.left(this)
  lazy val lastPost: ManyToOne[ForumPost] = FreekyDB.forumPostToForumLastPost.right(this)
  lazy val moderators = FreekyDB.forumsToModerators.left(this)
  lazy val readAccess = FreekyDB.forumsReadAccessToAccountType.left(this)
  lazy val writeAccess = FreekyDB.forumsWriteAccessToAccountType.left(this)
  
  def mkLink = <a href={"/forum/%d".format(id)}>{name}</a>
  
}