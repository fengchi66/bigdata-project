* [ValueState中存Map与MapState有什么区别](#valuestate中存map与mapstate有什么区别)
  * [1、 结论](#1-结论)
      * [性能](#性能)
      * [TTL](#ttl)
    * [举一反三](#举一反三)
  * [2、 State 中要存储哪些数据](#2-state-中要存储哪些数据)
    * [Key](#key)
    * [Namespace](#namespace)
    * [Value、UserKey、UserValue](#valueuserkeyuservalue)
  * [3、 StateBackend 中是如何存储和读写 State 数据的](#3-statebackend-中是如何存储和读写-state-数据的)
    * [3\.1 Heap 模式 ValueState 和 MapState 是如何存储的](#31-heap-模式-valuestate-和-mapstate-是如何存储的)
      * [回到正题：Heap 模式下，ValueState 中存 Map 与 MapState 有什么区别？](#回到正题heap-模式下valuestate-中存-map-与-mapstate-有什么区别)
    * [3\.2 RocksDB 模式 ValueState 和 MapState 是如何存储的](#32-rocksdb-模式-valuestate-和-mapstate-是如何存储的)
      * [3\.2\.1 ValueState 如何映射成 RocksDB 的 kv](#321-valuestate-如何映射成-rocksdb-的-kv)
      * [3\.2\.2 MapState 如何映射成 RocksDB 的 kv](#322-mapstate-如何映射成-rocksdb-的-kv)
    * [3\.3 RocksDB 模式下，ValueState 中存 Map 与 MapState 有什么区别？](#33-rocksdb-模式下valuestate-中存-map-与-mapstate-有什么区别)
      * [3\.3\.1 假设 Map 集合有 100 个 KV 键值对，具体两种方案会如何存储数据？](#331-假设-map-集合有-100-个-kv-键值对具体两种方案会如何存储数据)
      * [3\.3\.2 修改 Map 中的一个 KV 键值对的流程](#332-修改-map-中的一个-kv-键值对的流程)
      * [3\.3\.3 结论](#333-结论)
    * [3\.4 直接拼接 key 和 namespace 可能导致 RocksDB 的 key 冲突](#34-直接拼接-key-和-namespace-可能导致-rocksdb-的-key-冲突)
        * [解决方案：](#解决方案)
    * [3\.5 RocksDB 的 key 中还会存储 KeyGroupId](#35-rocksdb-的-key-中还会存储-keygroupid)
  * [4\. State TTL 简述](#4-state-ttl-简述)
    * [ValueState 中存 Map 与 MapState 有什么区别？](#valuestate-中存-map-与-mapstate-有什么区别)
  * [5\. 总结](#5-总结)
      * [性能](#性能-1)
      * [TTL](#ttl-1)

#  ValueState中存Map与MapState有什么区别

本文主要讨论一个问题：ValueState 中存 Map 与 MapState 有什么区别？

如果不懂这两者的区别，而且使用 ValueState 中存大对象，生产环境很可能会出现以下问题：

- CPU 被打满
- 吞吐上不去

## 1、 结论

从性能和 TTL 两个维度来描述区别。

#### 性能

- RocksDB 场景，MapState 比 ValueState 中存 Map 性能高很多

- - 生产环境强烈推荐使用 MapState，不推荐 ValueState 中存大对象
  - ValueState 中存大对象很容易使 CPU 打满

- Heap State 场景，两者性能类似

#### TTL

Flink 中 State 支持设置 TTL

- MapState 的 TTL 是基于 UK 级别的
- ValueState 的 TTL 是基于整个 key 的

### 举一反三

能使用 ListState 的场景，不要使用 ValueState 中存 List。

大佬们已经把 MapState 和 ListState 性能都做了很多优化，高性能不香吗？

**下文会详细分析 ValueState 和 MapState 底层的实现原理，通过分析原理得出上述结论。**

## 2、 State 中要存储哪些数据

ValueState 会存储 key、namespace、value，缩写为 <K, N, V>。

MapState 会存储 key、namespace、userKey、userValue，缩写为 <K, N, UK, UV>。

解释一下上述这些名词

### Key

ValueState 和 MapState 都是 KeyedState，也就是 keyBy 后才能使用 ValueState 和 MapState。所以 State 中肯定要保存 key。

例如：按照 app 进行 keyBy，总共有两个 app，分别是：app1 和 app2。那么状态存储引擎中肯定要存储 app1 或 app2，用于区分当前的状态数据到底是 app1 的还是 app2 的。

这里的 app1、app2 也就是所说的 key。

### Namespace

Namespace 用于区分窗口。

假设需要统计 app1 和 app2 每个小时的 pv 指标，则需要使用小时级别的窗口。状态引擎为了区分 app1 在 7 点和 8 点的 pv 值，就必须新增一个维度用来标识窗口。

Flink 用 Namespace 来标识窗口，这样就可以在状态引擎中区分出 app1 在 7 点和 8 点的状态信息。

### Value、UserKey、UserValue

ValueState 中存储具体的状态值。也就是上述例子中对应的 pv 值。

MapState 类似于 Map 集合，存储的是一个个 KV 键值对。为了与 keyBy 的 key 进行区分，所以 Flink 中把 MapState 的 key、value 分别叫 UserKey、UserValue。

下面讲述状态引擎是如何存储这些数据的。

## 3、 StateBackend 中是如何存储和读写 State 数据的

Flink 支持三种 StateBackend，分别是：MemoryStateBackend、FsStateBackend 和 RocksDBStateBackend。

其中 MemoryStateBackend、FsStateBackend 两种 StateBackend 在任务运行期间都会将 State 存储在内存中，两者在 Checkpoint 时将快照存储的位置不同。RocksDBStateBackend 在任务运行期间将 State 存储在本地的 RocksDB 数据库中。

所以下文将 MemoryStateBackend、FsStateBackend 统称为 heap 模式，RocksDBStateBackend 称为 RocksDB 模式。

### 3.1 Heap 模式 ValueState 和 MapState 是如何存储的

Heap 模式表示所有的状态数据都存储在 TM 的堆内存中，所有的状态都存储的原始对象，不会做序列化和反序列化。（注：Checkpoint 的时候会涉及到序列化和反序列化，数据的正常读写并不会涉及，所以这里先不讨论。）

Heap 模式下，无论是 ValueState 还是 MapState 都存储在 `CopyOnWriteStateMap<K, N, V>` 中。

key 、 Namespace 分别对应 CopyOnWriteStateMap 的 K、N。

ValueState 的 value 对应 CopyOnWriteStateMap 的 V。

MapState 将会把整个 Map 作为 CopyOnWriteStateMap 的 V，相当于 Flink 引擎创建了一个 HashMap 用于存储 MapState 的 KV 键值对。

具体 CopyOnWriteStateMap 是如何实现的，可以参考《[万字长文详解 Flink 中的 CopyOnWriteStateTable](https://mp.weixin.qq.com/s?__biz=MzkxOTE3MDU5MQ==&mid=2247484334&idx=1&sn=1aafab741bfd0e2e72652a4b459579c7&scene=21#wechat_redirect)》。

#### 回到正题：Heap 模式下，ValueState 中存 Map 与 MapState 有什么区别？

heap 模式下没有区别。

ValueState 中存 Map，相当于用户手动创建了一个 HashMap 当做 V 放到了状态引擎中。

而 MapState 是 Flink 引擎帮用户创建了一个 HashMap 当做 V 放到了状态引擎中。

所以实质上 ValueState 中存 Map 与 MapState 都是一样的，存储结构都是  `CopyOnWriteStateMap<K, N, HashMap>` 。区别在于 ValueState 是用户手动创建 HashMap，MapState 是 Flink 引擎创建 HashMap。

### 3.2 RocksDB 模式 ValueState 和 MapState 是如何存储的

RocksDB 模式表示所有的状态数据存储在 TM 本地的 RocksDB 数据库中。RocksDB 是一个 KV 数据库，且所有的 key 和 value 都是 byte 数组。所以无论是 ValueState 还是 MapState，存储到 RocksDB 中都必须将对象序列化成二进制当前 kv 存储在 RocksDB 中。

#### 3.2.1 ValueState 如何映射成 RocksDB 的 kv

ValueState 有 key、namespace、value 需要存储，所以最简单的思路：

1. 将 ValueState 的 key 序列化成 byte 数组
2. 将 ValueState 的 namespace 序列化成 byte 数组
3. 将两个 byte 数组拼接起来做为 RocksDB 的 key
4. 将 ValueState 的 value 序列化成 byte 数组做为 RocksDB 的 value

然后就可以写入到 RocksDB 中。

查询数据也用相同的逻辑：将 key 和 namespace 序列化后拼接起来作为 RocksDB 的 key，去 RocksDB 中进行查询，查询到的 byte 数组进行反序列化就得到了 ValueState 的 value。

这就是 RocksDB 模式下，ValueState 的读写流程。

#### 3.2.2 MapState 如何映射成 RocksDB 的 kv

MapState 有 key、namespace、userKey、userValue 需要存储，所以最简单的思路：

1. 将 MapState 的 key 序列化成 byte 数组
2. 将 MapState 的 namespace 序列化成 byte 数组
3. 将 MapState 的 userKey 序列化成 byte 数组
4. 将三个 byte 数组拼接起来做为 RocksDB 的 key
5. 将 MapState 的 value 序列化成 byte 数组做为 RocksDB 的 value

然后就可以写入到 RocksDB 中。

查询数据也用相同的逻辑：将 key、namespace、userKey 序列化后拼接起来作为 RocksDB 的 key，去 RocksDB 中进行查询，查询到的 byte 数组进行反序列化就得到了 MapState 的 userValue。

这就是 RocksDB 模式下，MapState 的读写流程。

### 3.3 RocksDB 模式下，ValueState 中存 Map 与 MapState 有什么区别？

#### 3.3.1 假设 Map 集合有 100 个 KV 键值对，具体两种方案会如何存储数据？

ValueState 中存 Map，Flink 引擎会把整个 Map 当做一个大 Value，存储在 RocksDB 中。对应到 RocksDB 中，100 个 KV 键值对的 Map 集合会序列化成一个 byte 数组当做 RocksDB 的 value，存储在 RocksDB 的 1 行数据中。

MapState 会根据 userKey，将 100 个 KV 键值对分别存储在 RocksDB 的 100 行中。

#### 3.3.2 修改 Map 中的一个 KV 键值对的流程

ValueState 的情况，虽然要修改 Map 中的一个 KV 键值对，但需要将整个 Map 集合从 RocksDB 中读出来。具体流程如下：

1. 将  key、namespace 序列化成 byte 数组，生成 RocksDB 的 key
2. 从 RocksDB 读出 key 对应 value 的 byte 数组
3. 将 byte 数组反序列化成整个 Map
4. 堆内存中修改 Map 集合
5. 将 Map 集合写入到 RocksDB 中，需要将整个 Map 集合序列化成 byte 数组，再写入

MapState 的情况，要修改 Map 中的一个 KV 键值对，根据 key、namespace、userKey 即可定位到要修改的那一个 KV 键值对。具体流程如下：

1. 将  key、namespace、userKey 序列化成 byte 数组，生成 RocksDB 的 key
2. 从 RocksDB 读出 key 对应 value 的 byte 数组
3. 将 byte 数组反序列化成 userValue
4. 堆内存中修改 userValue 的值
5. 将 userKey、userValue 写入到 RocksDB 中，需要先序列化，再写入

#### 3.3.3 结论

要修改 Map 中的一个 KV 键值对：

如果使用 ValueState 中存 Map，则每次修改操作需要序列化反序列化整个 Map 集合，每次序列化反序列大对象会非常耗 CPU，很容易将 CPU 打满。

如果使用 MapState，每次修改操作只需要序列化反序列化 userKey 那一个 KV 键值对的数据，效率较高。

举一反三：其他使用 ValueState、value 是大对象且 value 频繁更新的场景，都容易将 CPU 打满。

例如：ValueState 中存储的位图，如果每条数据都需要更新位图，则可能导致 CPU 被打满。

为了便于理解，上述忽略了一些实现细节，下面补充一下：

### 3.4 直接拼接 key 和 namespace 可能导致 RocksDB 的 key 冲突

假设 ValueState 中有两个数据：

key1 序列化后的二进制为 0x112233， namespace1 序列化后的二进制为0x4455

key2 序列化后的二进制为 0x1122， namespace2 序列化后的二进制为0x334455

这两个数据对应的 RocksDB key 都是 0x1122334455，这样的话，两个不同的 key、namespace 映射到 RocksDB 中变成了相同的数据，无法做区分。

##### 解决方案：

在 key 和 namespace 中间写入 key 的 byte 数组长度，在 namespace 后写入 namespace 的 byte 长度。

写入这两个长度就不可能出现 key 冲突了，具体为什么，读者可以自行思考。

### 3.5 RocksDB 的 key 中还会存储 KeyGroupId

对 KeyGroup 不了解的同学可以参考《[Flink 源码：从 KeyGroup 到 Rescale](https://mp.weixin.qq.com/s?__biz=MzkxOTE3MDU5MQ==&mid=2247484339&idx=1&sn=c83b5fc85f6abadaa7ac94f08c571b31&scene=21#wechat_redirect)》，加上 KeyGroupId 也比较简单。只需要修改 RocksDB key 的拼接方式，在序列化 key 和 namespace 之前，先序列化 KeyGroupId 即可。

## 4. State TTL 简述

Flink 中 TTL 的实现，都是将用户的 value 封装了一层，具体参考下面的 TtlValue 类：

```java
public class TtlValue<T> implements Serializable {
 @Nullable
 private final T userValue;
 private final long lastAccessTimestamp;
}
```

TtlValue 类中有两个字段，封装了用户的 value 且有一个时间戳字段，这个时间戳记录了这条数据写入的时间。

如果开启了 TTL，则状态中存储的 value 就是 TtlValue 对象。时间戳字段也会保存到状态引擎中，之后查询数据时，就可以通过该时间戳判断数据是否过期。

ValueState 将 value 封装为 TtlValue。

MapState 将 userValue 封装成 TtlValue。

ListState 将 element 封装成 TtlValue。

### ValueState 中存 Map 与 MapState 有什么区别？

如果 ValueState 中存 Map，则整个 Map 被当做 value，只维护一个时间戳。所以要么整个 Map 过期，要么都不过期。

MapState 中如果存储了 100 个 KV 键值对，则 100 个 KV 键值对都会存储各自的时间戳。因此每个 KV 键值对的 TTL 是相互独立的。

## 5. 总结

本文从实现原理详细分析了 ValueState 中存 Map 与 MapState 有什么区别？

从性能和 TTL 两个维度来描述两者的区别。

#### 性能

- RocksDB 场景，MapState 比 ValueState 中存 Map 性能高很多，ValueState 中存大对象很容易使 CPU 打满
- Heap State 场景，两者性能类似

#### TTL

Flink 中 State 支持设置 TTL，TTL 只是将时间戳与 userValue 封装起来。

- MapState 的 TTL 是基于 UK 级别的
- ValueState 的 TTL 是基于整个 key 的

扩展：其实 ListState 的数据映射到 RocksDB 比较复杂，用到了 RocksDB 的 merge 特性，比较有意思，有兴趣的同学可以阅读 RocksDB wiki《Merge Operator Implementation》，链接：https://github.com/facebook/rocksdb/wiki/Merge-Operator-Implementation