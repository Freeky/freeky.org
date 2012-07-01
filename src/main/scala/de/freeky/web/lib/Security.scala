package de.freeky.web.lib

import net.liftweb.util._
import Helpers._

object Security {
  def hashPassword(pw: String, salt: String): String = {
    hash("{" + pw + "} salt={" + salt + "}")
  }
  
  def generateSalt(): String =
    Safe.randomString(16)
    
}