package com.bigdata.flink.datastream.step

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.DataStream

abstract class Check[T :TypeInformation] {

  // 请子类取一个有意义的方法名的方法来调用map方法
  def map(input: DataStream[T]): DataStream[T] = {
    check(input).uid(s"check_${getUid}").name(s"check_${getName}")
  }

  def getUid: String

  def getName: String

  protected def check(input: DataStream[T]): DataStream[T]
}
