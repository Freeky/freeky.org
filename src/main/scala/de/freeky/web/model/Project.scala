package de.freeky.web.model

import net.liftweb.util.Helpers._
import org.squeryl.KeyedEntity
import org.squeryl.dsl.OneToMany
import java.sql.Timestamp

class Project(val id: Long,
    var name: String, 
    var description: String,
    var text: String,
    var modified: Timestamp) extends KeyedEntity[Long] {

  def this() = this(0, "", "", "", new Timestamp(millis))
}