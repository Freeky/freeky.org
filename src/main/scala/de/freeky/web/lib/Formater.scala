package de.freeky.web.lib
import java.text.SimpleDateFormat
import java.sql.Timestamp
import java.util.TimeZone

object Formater {

  val timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz")
  timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
  
  def format(timestamp: Timestamp) = {
    timestampFormat.format(timestamp)
  }
}