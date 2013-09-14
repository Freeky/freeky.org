package de.freeky.web.model

import org.squeryl.KeyedEntity
import net.liftweb.util.{Props, Helpers}
import Helpers._
import java.sql.Timestamp
import org.squeryl.dsl._

class ForumPost(val id: Long,
    var topicid: Long,
    var forumid: Long,
    val userid: Long,
    val time: Timestamp,
    var edit_time: Option[Timestamp],
    var edit_user_id: Option[Long],
    var edit_count: Option[Int],
    var text: String,
    var subject: String,
    var textile: Boolean) extends KeyedEntity[Long] {
  
  def this() = this(0L ,0L,0L ,0L, new Timestamp(0), Some(new Timestamp(0)), Some(0L), Some(0), "", "",true)
  def this(topicid: Long, forumid: Long, userid: Long, text: String, subject: String) =
    this(0, topicid, forumid, userid, new Timestamp(millis), None, None, None, text, subject, true)
    
  
  lazy val topic: ManyToOne[ForumTopic] = FreekyDB.forumTopicToForumPosts.right(this)
  lazy val forum: ManyToOne[Forum] = FreekyDB.forumToForumPosts.right(this)
  lazy val user: ManyToOne[User] = FreekyDB.userToForumPosts.right(this)
  lazy val edit_user: ManyToOne[User] = FreekyDB.userToForumPostEditUser.right(this)  
  
  def linkAddress = "/post/%d#p%d".format(id, id)
  def linkMarker = "p%d".format(id)
  def editLink = "/post/edit/%d".format(id)
}

object ForumPost {
  def mkLink(fp: ForumPost) = <a href={fp.linkAddress}>{fp.subject}</a>
}