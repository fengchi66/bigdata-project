# Flink TaskExecutor内存管理

## 一、 背景

- Flink 1.9及以前版本的TaskExecutor内存模型

  - 逻辑复杂难懂
  - 实际内存大小不确定
  - 不同场景及模块计算结果不一致
  - 批、流配置无法兼容

- Flink 1.10引入了FLIP-49，对TaskExecutor内存模型进行了统一的梳理和简化

- Flink 1.10内存模型：

  <img src="Flink TaskExecutor内存管理.assets/image-20200920223508792.png" alt="image-20200920223508792" style="zoom:50%;" />



## 二、Flink内存类型及用途

<img src="Flink TaskExecutor内存管理.assets/image-20200920223853718.png" alt="image-20200920223853718" style="zoom:50%;" />

> 左图是Flink1.10设计文档的内存模型图，有图为Flink 官方用户文档内存模型，实际上语义是一样的。
>
> 1. Process Memory: 一个Flink TaskExecutor进程使用的所有内存包含在内， 尤其是在容器化的环境中。
> 2. Flink Memory：抛出JVM自身的开销，留下的专门用于Flink应用程序的内存用量，通常用于standlone模式下。



###  Framework 和 Task Memory

- 区别：是否计入Slot资源

- 总用量受限: 

  - -Xmx = Framework Heap + Task Heap
  - -XX:MaxDirectMemorySize= Network + Framework Off-Heap + Task Off-Heap

- 无隔离

  <img src="Flink TaskExecutor内存管理.assets/image-20200921222757296.png" alt="image-20200921222757296" style="zoom:50%;" />

> 1. 设置JVM的参数，但我们并没有在Slot与Framework之间进行隔离，一个TaskExecutor是一个进程，不管TaskExecutor进行的是Slot也好，还是框架的其他任务也好，都是线程级别的任务。那对于JVM的内存特点来说，它是不会在线程级别对内存的使用量进行限制的。那为什么还要引入Framework与Task的区别呢？为后续版本的准备：动态切割Slot资源。
> 2. 不建议对Framework的内存进行调整，保留它的默认值。在Flink 1.10发布之前，它的默认值是通过跑一个空的Cluster，上面不调度任何任务的情况下，通过测量、计算出来的Framework所需要的内存。



### Heap 和 Off-Heap Memory

- Heap 
  - Java的堆内存，适用于绝大多数Java对象
  - HeapStateBackend
- Off-Heap
  - Direct: Java中的直接内存，但凡一下两种方式申请的内存，都属于Direct
    - DirectByteBuffer
    - MappedByteBuffer
  - Native
    - Native内存指的是像JNI、C/C++、Python、etc所用的不受Jvm进程管控的一些内存。

> Flink的配置模型没有要求用户对Direct和Native进行区分的，统一叫做Off-Heap。



### NetWork Memory：

- 用于
  - 数据传输缓冲
- 特点
  - 同一个TaskExecutor之间没有隔离
  - 需要多少由作业拓扑决定，不足会导致作业运行失败

> 1. 使用的是Direct Memory，只不过由于Network Memory在配置了指定大小之后，在集群启动的时候，它会去由已经配置的大小去把内存申请下来，并且在整个TaskExecutor Shutdown之前，都不会去释放内存。
> 2. 当通常考虑Network Memory用了多少，没有用多少的时候，这部分Network Memory实际上是一个常量，所以把它从Off-Heap里面拆出来单独管理。



### Managed Memory

- 用于
  - RocksDBStateBackend
  - Batch Operator
  - HeapStateBackend / 无状态应用，不需要Managed Memory，可以设置为0
- 特点
  - 同一TaskExecutor之间的多个Slot严格隔离
  - 多点少点都能跑，与性能挂钩
- RocksDB内存限制
  - state.backend.rocksdb.memory.m anaged (default: true)
  - 设置RocksDB实用的内存大小 Managed Memory 大小
  - 目的：避免容器内存超用，RocksDB的内存申请是在RocksDB内部完成的，Flink没有办法进行直接干预，但可以通过设置RocksDB的参数，让RocksDB去申请的内存大小刚好不超过Managed Memory。主要目的是为了防止state比较多(一个state一个列族)，RocksDB的内存超用，造成容器被Yarn/K8s Kill掉。

> 1. 本质上是用的Native Memory，并不会受到JVM的Heap、Direct大小的限制。不受JVM的掌控的，但是Flink会对它进行管理，Flink会严格控制到底申请了多少Managed Memory。
> 2. RocksDB实用C++写成的一个数据库，会用到Native Memory。
> 3. 一个Slot有多少Managed Memory，一定没有办法超用，这是其中一个特点；不管是RocksDB的用户，还是Batch Operator的用户，Task并没有一个严格要用多少大小的memory，可能会有一个最低限度，但一般会很小。



### JVM Metaspace

- 存放JVM加载的类的元数据
- 加载的类越多，需要空间越大
- 以下情况需要增大JVM Metaspace
  - 作业需要加载大量第三方库
  - 多个不同作业的任务运行在同一TaskExecutor上



### JVM Overhead

- Native Memory
- 用于Jvm其他开销
  - code cache
  - Thread Stack



## 三、内存特性解读

### Java内存类型

很多时候都在说，Flink使用到的内存包括Heap、Direct、Native Memory等，那么Java到底有多少种类型的内存？Java的内存分类是一个比较复杂的问题，但归根究底只有两种内存：Heap与Off-Heap。所谓Heap就是Java堆内存，Off-Heap就是堆外内存。

- Heap

  - 经过JVM虚拟化的内存。
  - 实际存储地址可能随GC变化，上层应用无感知。

  > 一个Java对象，或者一个应用程序拿到一个Java对象之后，它并不需要去关注内存实际上是存放在哪里的，实际上也没有一个固定的地址。

- Off-Heap
  - 未经JVM虚拟化的内存
  - 直接映射到本地OS内存地址

<img src="Flink TaskExecutor内存管理.assets/image-20200920230448872.png" alt="image-20200920230448872" style="zoom:50%;" />

> 1. Java的Heap内存空间实际上也是切分为了Eden、S0、S1、Tenured这几部分，所谓的垃圾回收机制会让对象在Eden空间中，随着Eden空间满了之后会触发GC，把已经没有使用的内存释放掉；已经引用的对象会移动到Servivor空间，在S0和S1之间反复复制，最后会放在老年代内存空间中。 这是Java的GC机制，造成的问题是Java对象会在内存空间中频繁的进行复制、拷贝。
> 2. 所谓的Off-Heap内存，是直接使用操作系统提供的内存，也就是说内存地址是操作系统来决定的。一方面避免了内存在频繁GC过程中拷贝的操作，另外如果涉及到对一些OS的映射或者网络的一些写出、文件的一些写出，它避免了在用户空间复制的一个成本，所以它的效率会相对更高。 
> 3. Heap的内存大小是有-Xmx决定的，而Off-Heap部分：有一些像Direct，它虽然不是在堆内，但是JVM会去对申请了多少Direct Memory进行计数、进行限制。如果设置了-XX：MaxDirectMemorySize的参数，当它达到限制的时候，就不会去继续申请新的内存;同样的对于metaspace也有同样的限制。
> 4. 既不受到Direct限制，也不受到Native限制的，是Native Memory。

- Question：什么是JVM内(外)的内存？

  - 经过JVM虚拟化
    - Heap

  - 受JVM管理
    - 用量上限、申请、释放（GC）等
    - Heap、Direct、Metaspace、even some Native（e.g., Thread Stack）

### Heap Memory特性

在Flink当中，Heap Memory

- 包括 
  - Framework Heap Memory
  - Task Heap Memory
- 用量上限受JVM严格控制
  - -Xmx：Framework Heap + Task Heap
  - 达到上限后触发垃圾回收（GC）
  - GC后仍然空间不足，触发OOM异常并退出
    - OutOfMemoryError: Java heap space

### Direct Memory特性

- 包括：
  - Framework Off-Heap Memory（部分）
  - Task Off-Heap Memory（部分）
  - Network Memory
- 用量上限受JVM严格控制
  - -XX：MaxDirectMemorySize
    - Framework Off-Heap + Task Off-Heap + Network Memory
  - 达到上限时触发GC，GC后仍然空间不足触发OOM异常并退出
    - OutOfMemoryError: Direct buffer memory

### Metaspace特性

- 用量上限受JVM严格控制
  - -XX：MaxMetaspaceSize
  - 达到上限时触发GC，GC后仍然空间不足触发OOM异常并退出
    - OutOfMemoryError: Metaspace

### Native Memory特性

- 包括
  - Framework Off-Heap Memory（小部分）
  - Task Off-Heap Memory（小部分）
  - Managed Memory
  - JVM Overhead
- 用量上限不受JVM严格控制
  - 其中Managed Memory用量上限受Flink严格控制



### Framework / Task Off-Heap Memory

- 既包括Direct也包括Native
- 用户无需理解Direct / Native内存的区别并分别配置
- 无法严格控制Direct内存用量，可能导致超用
- 绝大数情况下
  - Flink框架及用户代码不需要或只需要少量Native内存
  - Heap活动足够频繁，能够及时触发GC释放不需要的Direct内存

![image-20200921183651039](Flink TaskExecutor内存管理.assets/image-20200921183651039.png)

> 如果需要大量使用Native内存，可以考虑增大JVM Overhead



## 四、 配置方法

### Expilcit & Implicit

- Expilcit
  - Size,Min/Max
  - 严格保证（包括默认值）
  - 配置不当可能引发冲突

- Implicit
  - Fraction
  - 非严格保证
  - 存在冲突时优先保证Expilcit

> 1. 所有的size，min/max都是严格保证的，所谓严格保证：如果配置了一个size=100m， 配置结果一定是100m。如果配置了min/max是100-200m， 最终配置的内存大小一定在这个范围内。
>    1. 与之对应的Implicit，如果并不想配比如Managed Memory的内存大小，但是可以指定Managed Memory就是Flink Memory的一个比例。 



### 如何避免配置冲突

- 以下三项至少配置一项，不建议同时配置两项及以上
  - Total Process
  - Total Flink
  - Task Heap & Managed
- No Explicit Default

