package de.freeky.web.lib
import java.text.SimpleDateFormat
import java.sql.Timestamp
import java.util.TimeZone

object Formater {

  val timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz")
  val timestampFormatDate = new SimpleDateFormat("yyyy-MM-dd")
  val timestampFormatTime = new SimpleDateFormat("HH:mm:ss")
  timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
  
  def format(timestamp: Timestamp) = {
    timestampFormat.format(timestamp)
  }
  
  def formatTime(timestamp: Timestamp) = {
    timestampFormatTime.format(timestamp)
  }
  
  def formatDate(timestamp: Timestamp) = {
    timestampFormatDate.format(timestamp)
  }
}