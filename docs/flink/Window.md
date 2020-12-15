#  Window

回到Dataflow的思想，从流处理的角度来审视数据处理过程。对于无边界数据的处理，**`Where:Where in event time are results calculated?`** 计算什么时间(event time)范围的数据，答案是：通过使用pipeline中的event time窗口。

>事实上，Flink官网对Window的讲解以及使用已经足够详细了，总结这篇文章完全是多余，一定要阅读[**Flink官网-Window**](https://ci.apache.org/projects/flink/flink-docs-release-1.12/zh/dev/stream/operators/windows.html)。

### Window抽象概念

![image-20201215154505904](Window.assets/image-20201215154505904.png)

## Window分类

### Time Window

Window，也就是窗口，将一部分数据集合组合起操作。在处理无限数据集的时候有限操作需要窗口，比如 **aggregation**，**outer join**，**time-bounded** 操作。窗口大部分都是基于时间来划分，但是也有基于其他存在逻辑上有序关系的数据来划分的。窗口模型主要由三种：**Fixed Window**，**Sliding Window**，**Session Window**。

![stsy_0108](Window.assets/stsy_0108.png)

Flink对于窗口的通用定义:

- **Keyed Windows**

  ```java
  stream
         .keyBy(...)               <-  keyed versus non-keyed windows
         .window(...)              <-  required: "assigner"
        [.trigger(...)]            <-  optional: "trigger" (else default trigger)
        [.evictor(...)]            <-  optional: "evictor" (else no evictor)
        [.allowedLateness(...)]    <-  optional: "lateness" (else zero)
        [.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
         .reduce/aggregate/fold/apply()      <-  required: "function"
        [.getSideOutput(...)]      <-  optional: "output tag"
  ```

- **Non-Keyed Windows**

  ```scala
  stream
         .windowAll(...)           <-  required: "assigner"
        [.trigger(...)]            <-  optional: "trigger" (else default trigger)
        [.evictor(...)]            <-  optional: "evictor" (else no evictor)
        [.allowedLateness(...)]    <-  optional: "lateness" (else zero)
        [.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
         .reduce/aggregate/fold/apply()      <-  required: "function"
        [.getSideOutput(...)]      <-  optional: "output tag"
  ```

  

#### Fixed Window

Fixed Window ，有时候也叫 Tumbling Window。Tumble 的中文翻译有“翻筋斗”，我们可以将 Fixed Window 是特定的时间长度在无限数据集合上翻滚形成的，核心是每个 Window 没有重叠。比如小时窗口就是 12:00:00 ~ 13:00:00 一个窗口，13:00:00 ~ 14:00:00 一个窗口。从例子也可以看出来 Fixed Window 的另外一个特征：aligned，中文一般称为对齐。

特点：

- 将数据按照固定的窗口长度对数据进行切分。
- 时间对齐，窗口长度固定，没有重叠。

以下代码展示如何在传感数据流上定义事件事件和处理时间滚动窗口：

```scala
val sensorData: DataStream[SensorReading] = ...
// 基于事件时间的滚动窗口
val avgTemp = sensorData  
.keyBy(_.id)
.window(TumblingEventTimeWindows.of(Time.seconds(1)))
.process(new TemperatureAverager)

// 基于处理时间的滚动窗口
val avgTemp = sensorData  
.keyBy(_.id)
.window(TumblingProcessingTimeWindows.of(Time.seconds(1)))
.process(new TemperatureAverager)

val avgTemp = sensorData
.keyBy(_.id)
// 以上写法的简写，具体调用哪个方法取决于配置的时间特性。
// shortcut for window.(TumblingEventTimeWindows.of(size))
.timeWindow(Time.seconds(1))
.process(new TemperatureAverager)

```



#### Silding Window

Sliding Window，中文可以叫滑动窗口，由两个参数确定，窗口大小和滑动间隔。比如每分钟开始一个小时窗口对应的就是窗口大小为一小时，滑动间隔为一分钟。滑动间隔一般小于窗口大小，也就是说窗口之间会有重叠。滑动窗口在很多情况下都比较有用，比如检测机器的半小时负载，每分钟检测一次。Fixed Window 是 Sliding Window 的一种特例：窗口大小等于滑动间隔。

特点：

- 滑动窗口是固定窗口的更广义的一种形式，滑动窗口由固定的窗口长度和滑动间隔组成。

- 窗口长度固定，可以有重叠，也是一种对齐窗口。

- 处理时间滑动窗口分配器：

  ```scala
  // processing-time sliding windows assigner
  val slidingAvgTemp = sensorData
    .keyBy(_.id)
    // create 1h processing-time windows every 15 minutes
    .window(SlidingProcessingTimeWindows.of(Time.hours(1),
      Time.minutes(15)))
    .process(new TemperatureAverager)
  
  
  // 使用窗口分配器简写方法:
  // sliding windows assigner using a shortcut method
  val slidingAvgTemp = sensorData
    .keyBy(_.id)
  // shortcut for window.(SlidingEventTimeWindow.of(size,
  slide))
  .timeWindow(Time.hours(1), Time(minutes(15)))
  .process(new TemperatureAverager)
  ```

  

#### Session Window

Session Window，会话窗口， 会话由事件序列组成，这些事件序列以大于某个超时的不活动间隔终止（两边等），回话窗口不能事先定义，取决于数据流。一般用来捕捉一段时间内的行为，比如 Web 中一段时间内的登录行为为一个 Session，当长时间没有登录，则 Session 失效，再次登录重启一个 Session。Session Window 也是用超时时间来衡量，只要在超时时间内发生的事件都认为是一个 Session Window。

特点：

- 是一种非对齐窗口, Window Size可变，根据Session gap切分不同的窗口

- 使用场景，如：用户访问Session分析、基于Window的双流Join

- 基于Session Window实现双流的left join：

  ```scala
  input1.coGroup(input2)
          .where(_.order_item_id)
          .equalTo(_.item_id)
          .window(EventTimeSessionWindows.withGap(Time.seconds(5)))
          .apply(new CoGroupFunction）
  ```

### Count Window

#### Tumbling count window

```scala
keyedStream.countWindow(100)
```

#### Sliding count window

```scala
keyedStream.countWindow(100, 10)
```

如果对于DataStream，但并行度而言：

```scala
stream.countWindowAll(20, 10)
```

## 窗口函数

窗口函数定义了针对窗口内元素的计算逻辑。可用于窗口的函数类型有两种：

- 增量聚合函数： 它的应用场景是窗口内以状态形式存储某个值且需要根据每个加入窗口的元素对该值进行更新。来一条计算一条，将结果保存为状态。 特点：节省空间且最终会将聚合值作为单个结果发出。 ReduceFunction和AggregateFunction属于增量聚合函数。
- 全量窗口函数 收集窗口内所有元素，并在执行计算时对它们进行遍历。 通常占用更多空间，但支持更复杂的逻辑。 ProcessWindowFunction是全量窗口函数。

### RedeceFunction

- keyedStream -> dataStream

- 在被用到窗口内数据流时，会对窗口内元素进行增量聚合。

- 将聚合结构保存为一个状态。

- 要求输入、输出类型必须一致，所以仅用于一些简单聚合。



使用案例：在WindowedStream上应用reduce函数，计算每15秒的最低温度

```scala
val minTempPerWindow: DataStream[(String, Double)] = sensonData
  .map(r => (r.id, r.temperature))
  .keyBy(_._1)
  .timeWindow(Time.seconds(15))
  .reduce((r1, r2) => (r1._1, r1._2.min(r2._2)))

```



### AggregateFunction

- 和RedeceFunction类似，状态也是一个值。

- AggregateFunction接口:

  ```scala
  public interface AggregateFunction<IN, ACC, OUT> extends
          Function, Serializable {
      // create a new accumulator to start a new aggregate.
      ACC createAccumulator();
  
      // add an input element to the accumulator and return the accumulator.
      ACC add(IN value, ACC accumulator);
  
      // compute the result from the accumulator and return it.
      OUT getResult(ACC accumulator);
  
      // merge two accumulators and return the result.
      ACC merge(ACC a, ACC b);
  }
  ```

- 使用AggregateFunction计算每个窗口内传感器读数的平均温度。累加器负责维护不断变化的温度总和和数量。

  ```scala
  //    2.使用AggregateFunction计算每个窗口内传感器读数的平均温度。
      sensonData
        .map(r => (r.id, r.temperature))
        .keyBy(_._1)
        .timeWindow(Time.seconds(15))
        .aggregate(new AvgAggregateFunction())
  
  class AvgAggregateFunction() extends AggregateFunction[(String,Double),(String,Double,Int),(String,Double)]{
  //  初始化累加器
    override def createAccumulator(): (String, Double, Int) = ("",0.0,0)
  
  //  每来一条数据执行的逻辑，注意数据并行执行
    override def add(value: (String, Double), accumulator: (String, Double, Int)): (String, Double, Int) = (value._1,accumulator._2 + value._2,accumulator._3 +1)
  
    override def getResult(accumulator: (String, Double, Int)): (String, Double) = (accumulator._1,accumulator._2 / accumulator._3)
  
    override def merge(a: (String, Double, Int), b: (String, Double, Int)): (String, Double, Int) = (a._1,a._2 + b._2,a._3 + b._3)
  }
  
  ```

### ProcessWindowFunction

- 是一个全量窗口函数。

- 需要访问窗口内的所有元素来执行一些更加复杂的计算，例如计算窗口内数据的中值或出现频率最高的值。

- ProcessWindowFunction接口:

```scala
public abstract class ProcessWindowFunction<IN, OUT, KEY, W
        extends Window>
        extends AbstractRichFunction {
    // 对窗口执行计算
    void process(
            KEY key, Context ctx, Iterable<IN> vals, Collector<OUT>
            out) throws Exception;

    // 在窗口清除时删除自定义的单个窗口状态
    public void clear(Context ctx) throws Exception {
    }

    // 保存窗口元数据的上下文
    public abstract class Context implements Serializable {
        // 返回窗口的元数据
        public abstract W window();

        // 返回当前处理时间
        public abstract long currentProcessingTime();

        // 返回当前事件时间水位线
        public abstract long currentWatermark();

        // 用于单个窗口状态的访问器
        public abstract KeyedStateStore windowState();

        // 用于每个键值全局状态的访问器
        public abstract KeyedStateStore globalState();
        // 向OutputTag标识的副输出发送状态

        public abstract <X> void output(OutputTag<X> outputTag, X
                value);
    }
}

```

- 使用ProcessWindowFunction计算每个传感器在每个窗口内的最低温和最高温

  ```scala
  //  3.计算每个传感器在每个窗口内的最低温和最高温
  sensorData
    .keyBy(_.id)
    .timeWindow(Time.seconds(5))
    .process(new HighAndLowTempProcessFunction())
  
  case class MinMaxTemp(id:String,min:Double,max:Double,endTs:Long)
  
  class HighAndLowTempProcessFunction
    extends ProcessWindowFunction[SensorReading, MinMaxTemp, String, TimeWindow] {
  
    override def process(
                          key: String,
                          ctx: Context,
                          vals: Iterable[SensorReading],
                          out: Collector[MinMaxTemp]): Unit = {
  
      val temps = vals.map(_.temperature)
      val windowEnd = ctx.window.getEnd
  
      out.collect(MinMaxTemp(key, temps.min, temps.max, windowEnd))
    }
  }
  ```

- ProcessWindowFunction中的Context对象除了访问当前时间和事件时间、访问侧输出外，还提供了特有功能，如访问窗口的元数据，例如窗口中的开始时间和结束时间。

- 在系统内部，窗口中的所有事件会存储在ListState中。通过对所有事件收集起来且提供对于窗口元数据的访问及其他一些特性的访问和使用，所以使用场景比增量聚合更广泛。但收集全部状态的窗口其状态要大得多。

### 增量聚合与ProcessWindowFunction

- 增量集合函数计算逻辑，还需要访问窗口的元数据或状态。

- 实现上述过程的途径是将ProcessWindowFunction作为reduce()或aggregate()方法的第二个参数

- 前一个函数的输出即为后一个函数的输入即可。

  ```scala
  input
  .keyBy(...)
  .timeWindow(...)
  .reduce(
  incrAggregator: ReduceFunction[IN],
  function: ProcessWindowFunction[IN, OUT, K, W])
  
  input
  .keyBy(...)
  .timeWindow(...)
  .aggregate(
  incrAggregator: AggregateFunction[IN, ACC, V],
  windowFunction: ProcessWindowFunction[V, OUT, K, W])
  
  ```

- 示例： 计算每个传感器在每个窗口的温度的最大最小值。

  ```scala
  val minMaxTempPerWindow2: DataStream[MinMaxTemp] = sensorData
    .map(r => (r.id, r.temperature, r.temperature))
    .keyBy(_._1)
    .timeWindow(Time.seconds(5))
    .reduce(
      // incrementally compute min and max temperature
      (r1: (String, Double, Double), r2: (String, Double, Double)) => {
        (r1._1, r1._2.min(r2._2), r1._3.max(r2._3))
      },
      // finalize result in ProcessWindowFunction
      new AssignWindowEndProcessFunction()
    )
  
  class AssignWindowEndProcessFunction
    extends ProcessWindowFunction[(String, Double, Double), MinMaxTemp, String, TimeWindow] {
  
    override def process(
                          key: String,
                          ctx: Context,
                          minMaxIt: Iterable[(String, Double, Double)],
                          out: Collector[MinMaxTemp]): Unit = {
  
      val minMax = minMaxIt.head
      val windowEnd = ctx.window.getEnd
      out.collect(MinMaxTemp(key, minMax._2, minMax._3, windowEnd))
    }
  }
  
  ```

  

 ## Window Assigner

 Flink 窗口的结构中有两个必须的两个操作：

- 使用窗口分配器（WindowAssigner）将数据流中的元素分配到对应的窗口。

- 当满足窗口触发条件后，对窗口内的数据使用窗口处理函数（Window Function）进行处理，常

  用的 Window Function 有 reduce、aggregate、process。

![image-20201215114037836](Window.assets/image-20201215114037836.png)

对于KeyedStream，各种窗口分配器使用:

- Tumbling time window

  ```scala
  keyedStream.timeWindow(Time.minutes(1))
  ```

- Sliding time window

  ```scala
  keyedStream.timeWindow(Time.minutes(1), Time.seconds(10))
  ```

- Tumbling count window

  ```scala
  keyedStream.countWindow(100)
  ```

- Sliding count window

  ```scala
  keyedStream.countWindow(100, 10)
  ```

- Session window

  ```scala
  keyedStream.window(EventTimeSessionWindows.withGap(Time. seconds(3))
  ```



对于DataStream，窗口分配器使用：

```scala
stream.windowAll(…)…
stream.timeWindowAll(Time.seconds(10))…
stream.countWindowAll(20, 10)…
```



## Window Trigger

触发器（Trigger）决定了何时启动 Window Function 来处理窗口中的数据以及何时将窗口内的数据清理。每个`WindowAssigner`都有一个默认`Trigger`

| Flink 内置 Window Trigger       | 触发频率 | 主要功能                                                     |
| ------------------------------- | -------- | ------------------------------------------------------------ |
| ProcessingTimeTrigger           | 一次触发 | 基于 ProcessingTime 触发，当机器时间大于窗口结束时间时触发   |
| EventTimeTrigger                | 一次触发 | 基于 EventTime，当 Watermark 大于窗口技术时间时触发          |
| ContinuousProcessingTimeTrigger | 多次触发 | 基于 ProcessTime 的固定时间间隔触发                          |
| ContinuousEventTimeTrigger      | 多次触发 | 基于 EventTime 的固定时间间隔触发                            |
| CountTrigger                    | 多次触发 | 基于 Element 的固定条数触发                                  |
| DeltaTrigger                    | 多次触发 | 基于本次 Element和上次触发 Trigger 的 Element 做Delta 计算，超过指定 Threshold 后触发 |
| PuringTrigger                   |          | 对 Trigger 的封装实现，用于 Trigger 触发后额外清理中间状态数据 |

- 整个Flink里面内置了非常多的Window Trigger,这里面包括基于 ProcessingTime 触发的ProcessingTimeTrigger以及基于 EventTime触发的EventTimeTrigger,对于这两种Window Trigger来说，基本上可以满足大部分的窗口的触发的逻辑。
- 在上面的基础之上，又延伸出来两种，叫做ContinuousProcessingTimeTrigger和ContinuousEventTimeTrigger，他们的特点是多次触发，比如ContinuousEventTimeTrigger会基于Event time的**`固定时间间隔`**触发。
- CountTrigger，是基于接入事件元素的固定条数，比如说每接入100条触发一次，那么Elements的固定条数，就是CountTrigger里面所需要依赖的条件。
- DeltaTrigger，是基于我们本次数据元素和上次触发Trigger的数据元素之间做一个Delta的计算，Delta计算出来的结果会和一个指定的Threshold进行对比，如果超过了指定的Threshold指标，此时窗口触发计算。
- PuringTrigger，是需要去基于前面提到的

### Window Trigger触发机制

先来看看在实际生产环境中最常用的Trigger：EventTimeTrigger是如何工作的

#### EventTimeTrigger触发机制

以下案例中，左边是输入的数据，event time从12：00到12：10，它的指标有1、2、3、4、5，对应的Watermark的延迟限制是2分钟，这个时候会通过window assigner去分配对应的窗口，这里定义滚动窗口，并定义窗口大小是5分钟。

- 当12：00，1这条数据进入第一个窗口时，窗口中有一个状态的维护，EventTimeTrigger会去控制窗口window function的计算以及结果的输出，包括Window Result的输出。

- 一直到12：08，4的数据进入第二个窗口的时候，此时将watermark更新为12：06，大于第一个窗口的结束时间，触发窗口计算，发出结果。

  ![image-20201215172027772](Window.assets/image-20201215172027772.png)

