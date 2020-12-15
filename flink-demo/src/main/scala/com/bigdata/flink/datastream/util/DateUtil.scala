package com.bigdata.flink.datastream.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object DateUtil {

  import java.time.format.DateTimeFormatter

  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  def formatTsToString(milliseconds: Long): String = {
    dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault()))
  }

  def formatStringToTs(time: String): Long = {
    val localDateTime1 = LocalDateTime.parse(time, dtf)
    LocalDateTime.from(localDateTime1).atZone(ZoneId.systemDefault).toInstant.toEpochMilli
  }

  def main(args: Array[String]): Unit = {
    println(formatStringToTs("2020-12-02 11:10"))

    println(formatTsToString(formatStringToTs("2020-12-02 11:10")))
  }

}
