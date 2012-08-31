package de.freeky.web.model

import org.squeryl.KeyedEntity
import java.sql.Timestamp
import net.liftweb.util.Helpers._

class StaticPage(val id: Long, 
    val name: String, 
    var content: String, 
    var lastModified: Timestamp,
    var description: String,
    var keywords: String,
    var title: String) extends KeyedEntity[Long] {
  
  def this() = this(0, "", "", new Timestamp(millis), "", "", "")
}