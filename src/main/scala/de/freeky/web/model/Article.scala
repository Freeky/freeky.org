package de.freeky.web.model


import net.liftweb.util.Helpers._
import java.sql.Timestamp

import org.squeryl.KeyedEntity
import org.squeryl.dsl._
import org.squeryl.PrimitiveTypeMode._


abstract class Article() extends KeyedEntity[Long] {
	val id: Long
    var title: String 
    var text: String
    var authorId: Long
    val created: Timestamp
    var modified: Timestamp 
    var published: Option[Timestamp]

    val author: ManyToOne[User] 
	
	
	
}

abstract class ArticleFactory[U <: Article]() {
  def create: U
}