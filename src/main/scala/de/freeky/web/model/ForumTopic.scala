package de.freeky.web.model

import org.squeryl.KeyedEntity
import net.liftweb.util.{Props, Helpers}
import Helpers._
import java.sql.Timestamp
import org.squeryl.dsl._
import scala.xml.Text

class ForumTopic(val id: Long,
    var forumid: Long,
    var title: String,
    var userid: Long,
    var views: Int,
    var replies: Int,
    val time: Timestamp,
    var closed: Option[Timestamp],
    var isSticky: Boolean) extends KeyedEntity[Long]{

  def this(forumid: Long, title: String, userid: Long) = this(0, forumid, title, userid, 0, 0, new Timestamp(millis), None, false)
  def this() = this(0, 0, "", 0, 0, 0, new Timestamp(millis), Some(new Timestamp(millis)), false)
  
  lazy val forum: ManyToOne[Forum] = FreekyDB.forumToForumTopics.right(this)
  lazy val user: ManyToOne[User] = FreekyDB.userToForumTopics.right(this)
  lazy val posts: OneToMany[ForumPost] = FreekyDB.forumTopicToForumPosts.left(this)
  
  def linkAddress = "/topic/%d".format(id)
  def answearAddress = "/topic/answear/%d".format(id)
}

object ForumTopic {
  def mkLink(ft: ForumTopic) = 
  <a href={ft.linkAddress}>{ft.title}</a>
}