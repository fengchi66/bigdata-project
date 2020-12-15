package com.bigdata.flink.datastream.windowing

import com.bigdata.flink.datastream.util.{DateUtil, UserEventCount}
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.triggers.{ContinuousEventTimeTrigger, EventTimeTrigger, PurgingTrigger}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector


/**
 * 测试EventTimeTrigger，触发时机以及窗口函数的简单使用
 */
object EventTimeTriggerDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.enableCheckpointing(120000)
    env.setParallelism(1)

    val list = List(
      UserEventCount(1, DateUtil.formatStringToTs("2020-12-15 12:00"), 1),
      UserEventCount(1, DateUtil.formatStringToTs("2020-12-15 12:04"), 2),
      UserEventCount(1, DateUtil.formatStringToTs("2020-12-15 12:03"), 3),
      UserEventCount(1, DateUtil.formatStringToTs("2020-12-15 12:08"), 4),
      UserEventCount(1, DateUtil.formatStringToTs("2020-12-15 12:10"), 5)
    )

    env.fromCollection(list)
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[UserEventCount](Time.minutes(2)) {
      override def extractTimestamp(t: UserEventCount): Long = t.timestamp
    })
        .keyBy(_.id)
        .window(TumblingEventTimeWindows.of(Time.minutes(5)))
//        .trigger(EventTimeTrigger.create())
//        .trigger(ContinuousEventTimeTrigger.of(Time.minutes(2)))
//        .trigger(PurgingTrigger.of(ContinuousEventTimeTrigger.of(Time.minutes(2))))
        .process(new WindowFunc)
        .print()

    env.execute("job")
  }

  class AggCountFunc extends AggregateFunction[UserEventCount, (Int, Int), (Int, Int)] {
    override def createAccumulator(): (Int, Int) = (0, 0)

    override def add(in: UserEventCount, acc: (Int, Int)): (Int, Int) = (in.id, in.count + acc._2)

    override def getResult(acc: (Int, Int)): (Int, Int) = acc

    override def merge(acc: (Int, Int), acc1: (Int, Int)): (Int, Int) = (acc._1, acc._2 + acc1._2)
  }

  class WindowResult extends ProcessWindowFunction[(Int, Int), (String, Int, Int), Int, TimeWindow] {
    override def process(key: Int, context: Context, elements: Iterable[(Int, Int)], out: Collector[(String, Int, Int)]): Unit = {
      // 输出窗口开始时间
      val windowStart = DateUtil.formatTsToString(context.window.getStart)
      val userId = elements.head._1
      val count = elements.head._2
      out.collect((windowStart, userId, count))
    }
  }

  class WindowFunc extends ProcessWindowFunction[UserEventCount, (String, Int, Int), Int, TimeWindow] {
    override def process(key: Int, context: Context, elements: Iterable[UserEventCount], out: Collector[(String, Int, Int)]): Unit = {
      val count = elements.map(_.count).sum
      val windowStart = DateUtil.formatTsToString(context.window.getStart)
      out.collect((windowStart, key, count))
    }
  }

}
