package de.freeky.web.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2
import org.squeryl.PrimitiveTypeMode.compositeKey

class ForumWriteAccess(val forumId: Long,
    val accountTypeId: Long) extends KeyedEntity[CompositeKey2[Long,Long]] {
	def id = compositeKey(forumId, accountTypeId)

}