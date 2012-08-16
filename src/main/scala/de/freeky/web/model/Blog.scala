package de.freeky.web.model


import net.liftweb.util.Helpers._
import java.sql.Timestamp
import org.squeryl.KeyedEntity
import org.squeryl.dsl.ManyToOne


class Blog(val id: Long,
    var title: String, 
    var text: String,
    var authorId: Long,
    val created: Timestamp,
    var modified: Timestamp) extends Article {
  
  def this() = this(0L, "", "", 0L, new Timestamp(millis), new Timestamp(millis))
  
  lazy val author: ManyToOne[User] = FreekyDB.userToBlogs.right(this)

}

class BlogFactory extends ArticleFactory[Blog] {
  def create = new Blog()
}