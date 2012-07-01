package de.freeky.web.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl.OneToMany

class Project(val id: Long,
    var name: String, 
    var description: String,
    var text: String) extends KeyedEntity[Long] {

  def this() = this(0, "", "", "")
}