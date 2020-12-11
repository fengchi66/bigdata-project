* [ClickHouse的数据结构](#clickhouse的数据结构)
  * [数值类型](#数值类型)
    * [Int类型](#int类型)
    * [浮点类型](#浮点类型)
    * [Decimal类型](#decimal类型)
  * [字符串类型](#字符串类型)
    * [String](#string)
    * [FixedString](#fixedstring)
    * [UUID](#uuid)
  * [日期类型](#日期类型)
    * [Date类型](#date类型)
    * [DateTime类型](#datetime类型)
  * [布尔类型](#布尔类型)
  * [数组类型](#数组类型)
  * [枚举类型](#枚举类型)
  * [Tuple类型](#tuple类型)
  * [特殊数据类型](#特殊数据类型)
    * [Nullable](#nullable)
    * [Domain](#domain)


# ClickHouse的数据结构

ClickHouse提供了许多数据类型，它们可以划分为基础类型、复合类型和特殊类型。我们可以在`system.data_type_families`表中检查数据类型名称以及是否区分大小写。

```
SELECT * FROM system.data_type_families
```

上面的系统表，存储了ClickHouse所支持的数据类型，注意不同版本的ClickHouse可能数据类型会有所不同，具体如下表所示：

```
┌─name────────────────────┬─case_insensitive─┬─alias_to────┐
│ IPv6                    │                0 │             │
│ IPv4                    │                0 │             │
│ LowCardinality          │                0 │             │
│ Decimal                 │                1 │             │
│ String                  │                0 │             │
│ Decimal64               │                1 │             │
│ Decimal32               │                1 │             │
│ Decimal128              │                1 │             │
│ Float64                 │                0 │             │
│ Float32                 │                0 │             │
│ Int64                   │                0 │             │
│ SimpleAggregateFunction │                0 │             │
│ Array                   │                0 │             │
│ Nothing                 │                0 │             │
│ UInt16                  │                0 │             │
│ Enum16                  │                0 │             │
│ UInt32                  │                0 │             │
│ Date                    │                1 │             │
│ Int8                    │                0 │             │
│ Int32                   │                0 │             │
│ Enum8                   │                0 │             │
│ UInt64                  │                0 │             │
│ IntervalSecond          │                0 │             │
│ Int16                   │                0 │             │
│ FixedString             │                0 │             │
│ Nullable                │                0 │             │
│ AggregateFunction       │                0 │             │
│ DateTime                │                1 │             │
│ Enum                    │                0 │             │
│ Tuple                   │                0 │             │
│ IntervalMonth           │                0 │             │
│ Nested                  │                0 │             │
│ IntervalMinute          │                0 │             │
│ IntervalHour            │                0 │             │
│ IntervalWeek            │                0 │             │
│ IntervalDay             │                0 │             │
│ UInt8                   │                0 │             │
│ IntervalQuarter         │                0 │             │
│ UUID                    │                0 │             │
│ IntervalYear            │                0 │             │
│ LONGBLOB                │                1 │ String      │
│ MEDIUMBLOB              │                1 │ String      │
│ TINYBLOB                │                1 │ String      │
│ BIGINT                  │                1 │ Int64       │
│ SMALLINT                │                1 │ Int16       │
│ TIMESTAMP               │                1 │ DateTime    │
│ INTEGER                 │                1 │ Int32       │
│ INT                     │                1 │ Int32       │
│ DOUBLE                  │                1 │ Float64     │
│ MEDIUMTEXT              │                1 │ String      │
│ TINYINT                 │                1 │ Int8        │
│ DEC                     │                1 │ Decimal     │
│ BINARY                  │                1 │ FixedString │
│ FLOAT                   │                1 │ Float32     │
│ CHAR                    │                1 │ String      │
│ VARCHAR                 │                1 │ String      │
│ TEXT                    │                1 │ String      │
│ TINYTEXT                │                1 │ String      │
│ LONGTEXT                │                1 │ String      │
│ BLOB                    │                1 │ String      │
└─────────────────────────┴──────────────────┴─────────────┘
```

## 数值类型

### Int类型

固定长度的整数类型又包括有符号和无符号的整数类型。

- 有符号整数类型

| 类型   | 字节 | 范围               |
| :----- | :--- | :----------------- |
| Int8   | 1    | [-2^7 ~2^7-1]      |
| Int16  | 2    | [-2^15 ~ 2^15-1]   |
| Int32  | 4    | [-2^31 ~ 2^31-1]   |
| Int64  | 8    | [-2^63 ~ 2^63-1]   |
| Int128 | 16   | [-2^127 ~ 2^127-1] |
| Int256 | 32   | [-2^255 ~ 2^255-1] |

- 无符号类型

| 类型    | 字节 | 范围          |
| :------ | :--- | :------------ |
| UInt8   | 1    | [0 ~2^8-1]    |
| UInt16  | 2    | [0 ~ 2^16-1]  |
| UInt32  | 4    | [0 ~ 2^32-1]  |
| UInt64  | 8    | [0 ~ 2^64-1]  |
| UInt256 | 32   | [0 ~ 2^256-1] |

### 浮点类型

- 单精度浮点数

Float32从小数点后第8位起会发生数据溢出

| 类型    | 字节 | 精度 |
| :------ | :--- | :--- |
| Float32 | 4    | 7    |

- 双精度浮点数

Float32从小数点后第17位起会发生数据溢出

| 类型    | 字节 | 精度 |
| :------ | :--- | :--- |
| Float64 | 8    | 16   |

- 示例

```
-- Float32类型，从第8为开始产生数据溢出
kms-1.apache.com :) select toFloat32(0.123456789);

SELECT toFloat32(0.123456789)

┌─toFloat32(0.123456789)─┐
│             0.12345679 │
└────────────────────────┘
-- Float64类型，从第17为开始产生数据溢出
kms-1.apache.com :) select toFloat64(0.12345678901234567890);

SELECT toFloat64(0.12345678901234568)

┌─toFloat64(0.12345678901234568)─┐
│            0.12345678901234568 │
└────────────────────────────────┘
```

### Decimal类型

有符号的定点数，可在加、减和乘法运算过程中保持精度。ClickHouse提供了Decimal32、Decimal64和Decimal128三种精度的定点数，支持几种写法：

- Decimal(P, S)

- Decimal32(S)

  **数据范围：( -1 \* 10^(9 - S), 1 \* 10^(9 - S) )**

- Decimal64(S)

  **数据范围：( -1 \* 10^(18 - S), 1 \* 10^(18 - S) )**

- Decimal128(S)

  **数据范围：( -1 \* 10^(38 - S), 1 \* 10^(38 - S) )**

- Decimal256(S)

  **数据范围：( -1 \* 10^(76 - S), 1 \* 10^(76 - S) )**

其中：**P**代表精度，决定总位数（整数部分+小数部分），取值范围是1～76

**S**代表规模，决定小数位数，取值范围是0～P

根据**P**的范围，可以有如下的等同写法：

| P 取值      | 原生写法示例  | 等同于        |
| :---------- | :------------ | :------------ |
| [ 1 : 9 ]   | Decimal(9,2)  | Decimal32(2)  |
| [ 10 : 18 ] | Decimal(18,2) | Decimal64(2)  |
| [ 19 : 38 ] | Decimal(38,2) | Decimal128(2) |
| [ 39 : 76 ] | Decimal(76,2) | Decimal256(2) |

**注意点**：不同精度的数据进行四则运算时，**精度(总位数)和规模(小数点位数)**会发生变化，具体规则如下：

- 精度对应的规则

  **可以看出：两个不同精度的数据进行四则运算时，结果数据已最大精度为准**

- - Decimal64(S1) `运算符` Decimal32(S2)  ->  Decimal64(S)
  - Decimal128(S1) `运算符` Decimal32(S2) -> Decimal128(S）
  - Decimal128(S1) `运算符` Decimal64(S2) -> Decimal128(S)
  - Decimal256(S1) `运算符` Decimal<32|64|128>(S2) -> Decimal256(S)

- 规模(小数点位数)对应的规则

- - 加法|减法：S = max(S1, S2)，即以两个数据中小数点位数最多的为准
  - 乘法：S = S1 + S2(注意：S1精度 >= S2精度)，即以两个数据的小数位相加为准

- 除法：S = S1，即被除数的小数位为准

```
  -- toDecimal32(value, S)
  -- 加法,S取两者最大的，P取两者最大的
  SELECT
      toDecimal64(2, 3) AS x,
      toTypeName(x) AS xtype,
      toDecimal32(2, 2) AS y,
      toTypeName(y) as ytype,
      x + y AS z,
      toTypeName(z) AS ztype;
  -- 结果输出
  ┌─────x─┬─xtype──────────┬────y─┬─ytype─────────┬─────z─┬─ztype──────────┐
  │ 2.000 │ Decimal(18, 3) │ 2.00 │ Decimal(9, 2) │ 4.000 │ Decimal(18, 3) │
  └───────┴────────────────┴──────┴───────────────┴───────┴────────────────┘
  -- 乘法，比较特殊，与这两个数的顺序有关
  -- 如下：x类型是Decimal64，y类型是Decimal32，顺序是x*y,小数位S=S1+S2
  SELECT
      toDecimal64(2, 3) AS x,
      toTypeName(x) AS xtype,
      toDecimal32(2, 2) AS y,
      toTypeName(y) as ytype,
      x * y AS z,
      toTypeName(z) AS ztype;
  -- 结果输出   
  ┌─────x─┬─xtype──────────┬────y─┬─ytype─────────┬───────z─┬─ztype──────────┐
  │ 2.000 │ Decimal(18, 3) │ 2.00 │ Decimal(9, 2) │ 4.00000 │ Decimal(18, 5) │
  └───────┴────────────────┴──────┴───────────────┴─────────┴────────────────┘
  -- 交换相乘的顺序，y*x,小数位S=S1*S2
  SELECT
      toDecimal64(2, 3) AS x,
      toTypeName(x) AS xtype,
      toDecimal32(2, 2) AS y,
      toTypeName(y) as ytype,
      y * x AS z,
      toTypeName(z) AS ztype;
  -- 结果输出
  ┌─────x─┬─xtype──────────┬────y─┬─ytype─────────┬────────z─┬─ztype──────────┐
  │ 2.000 │ Decimal(18, 3) │ 2.00 │ Decimal(9, 2) │ 0.400000 │ Decimal(18, 6) │
  └───────┴────────────────┴──────┴───────────────┴──────────┴────────────────┘
  -- 除法，小数位与被除数保持一致
  SELECT
      toDecimal64(2, 3) AS x,
      toTypeName(x) AS xtype,
      toDecimal32(2, 2) AS y,
      toTypeName(y) as ytype,
      x / y AS z,
      toTypeName(z) AS ztype;
  -- 结果输出
  ┌─────x─┬─xtype──────────┬────y─┬─ytype─────────┬─────z─┬─ztype──────────┐
  │ 2.000 │ Decimal(18, 3) │ 2.00 │ Decimal(9, 2) │ 1.000 │ Decimal(18, 3) │
  └───────┴────────────────┴──────┴───────────────┴───────┴────────────────┘
```

## 字符串类型

### String

字符串可以是任意长度的。它可以包含任意的字节集，包含空字节。因此，字符串类型可以代替其他 DBMSs 中的VARCHAR、BLOB、CLOB 等类型。

### FixedString

固定长度的`N`字节字符串，一般在在一些明确字符串长度的场景下使用，声明方式如下：

```
-- N表示字符串的长度
<column_name> FixedString(N)
```

值得注意的是：FixedString使用null字节填充末尾字符。

```
-- 虽然hello只有5位，但最终字符串是6位
select toFixedString('hello',6) as a,length(a) as alength;
-- 结果输出

┌─a─────┬─alength─┐
│ hello │       6 │
└───────┴─────────┘
-- 注意对于固定长度的字符串进行比较时，会出现不一样的结果
-- 如下：由于a是6位字符，而hello是5位，所以两者不相等
select toFixedString('hello',6) as a,a = 'hello' ,length(a) as alength;    
-- 结果输出
┌─a─────┬─equals(toFixedString('hello', 6), 'hello')─┬─alength─┐
│ hello │                                          0 │       6 │
└───────┴────────────────────────────────────────────┴─────────┘

-- 需要使用下面的方式
select toFixedString('hello',6) as a,a = 'hello\0' ,length(a) as alength;
-- 结果输出

┌─a─────┬─equals(toFixedString('hello', 6), 'hello\0')─┬─alength─┐
│ hello │                                            1 │       6 │
└───────┴──────────────────────────────────────────────┴─────────┘
```

### UUID

UUID是一种数据库常见的主键类型，在ClickHouse中直接把它作为一种数据类型。UUID共有32位，它的格式为8-4-4-4-12，比如：

```
61f0c404-5cb3-11e7-907b-a6006ad3dba0
-- 当不指定uuid列的值时，填充为0
00000000-0000-0000-0000-000000000000
```

使用示例如下：

```
-- 建表
CREATE TABLE t_uuid (x UUID, y String) ENGINE=TinyLog;
-- insert数据
INSERT INTO t_uuid SELECT generateUUIDv4(), 'Example 1';
INSERT INTO t_uuid (y) VALUES ('Example 2');
SELECT * FROM t_uuid;
-- 结果输出，默认被填充为0
┌────────────────────────────────────x─┬─y─────────┐
│ b6b019b5-ee5c-4967-9c4d-8ff95d332230 │ Example 1 │
│ 00000000-0000-0000-0000-000000000000 │ Example 2 │
└──────────────────────────────────────┴───────────┘
```

## 日期类型

时间类型分为DateTime、DateTime64和Date三类。需要注意的是ClickHouse目前没有时间戳类型，也就是说，时间类型最高的精度是秒，所以如果需要处理毫秒、微秒精度的时间，则只能借助UInt类型实现。

### Date类型

用两个字节存储，表示从 1970-01-01 (无符号) 到当前的日期值。日期中没有存储时区信息。

```
CREATE TABLE t_date (x date) ENGINE=TinyLog;
INSERT INTO t_date VALUES('2020-10-01');
SELECT x,toTypeName(x) FROM t_date;
┌──────────x─┬─toTypeName(x)─┐
│ 2020-10-01 │ Date          │
└────────────┴───────────────┘
```

### DateTime类型

用四个字节（无符号的）存储 Unix 时间戳。允许存储与日期类型相同的范围内的值。最小值为 0000-00-00 00:00:00。时间戳类型值精确到秒（没有闰秒）。时区使用启动客户端或服务器时的系统时区。

```
CREATE TABLE t_datetime(`timestamp` DateTime) ENGINE = TinyLog;
INSERT INTO t_datetime Values('2020-10-01 00:00:00');
SELECT * FROM t_datetime;
-- 结果输出
┌───────────timestamp─┐
│ 2020-10-01 00:00:00 │
└─────────────────────┘
-- 注意，DateTime类型是区分时区的
SELECT 
    toDateTime(timestamp, 'Asia/Shanghai') AS column,
    toTypeName(column) AS x 
FROM t_datetime;
-- 结果输出
┌──────────────column─┬─x─────────────────────────┐
│ 2020-10-01 00:00:00 │ DateTime('Asia/Shanghai') │
└─────────────────────┴───────────────────────────┘
SELECT
     toDateTime(timestamp, 'Europe/Moscow') AS column,
     toTypeName(column) AS x 
FROM t_datetime; 
-- 结果输出
┌──────────────column─┬─x─────────────────────────┐
│ 2020-09-30 19:00:00 │ DateTime('Europe/Moscow') │
└─────────────────────┴───────────────────────────┘
```

## 布尔类型

ClickHouse没有单独的类型来存储布尔值。可以使用UInt8 类型，取值限制为0或 1。

## 数组类型

Array(T)，由 T 类型元素组成的数组。T 可以是任意类型，包含数组类型。但不推荐使用多维数组，ClickHouse对多维数组的支持有限。例如，不能在MergeTree表中存储多维数组。

```
SELECT array(1, 2) AS x, toTypeName(x);
-- 结果输出
┌─x─────┬─toTypeName(array(1, 2))─┐
│ [1,2] │ Array(UInt8)            │
└───────┴─────────────────────────┘
SELECT [1, 2] AS x, toTypeName(x);
-- 结果输出
┌─x─────┬─toTypeName([1, 2])─┐
│ [1,2] │ Array(UInt8)       │
└───────┴────────────────────┘
```

需要注意的是，数组元素中如果存在Null值，则元素类型将变为Nullable。

```
SELECT array(1, 2, NULL) AS x, toTypeName(x);
-- 结果输出
┌─x──────────┬─toTypeName(array(1, 2, NULL))─┐
│ [1,2,NULL] │ Array(Nullable(UInt8))        │
└────────────┴───────────────────────────────┘
```

另外，数组类型里面的元素必须具有相同的数据类型，否则会报异常

```
SELECT array(1, 'a')
-- 报异常
DB::Exception: There is no supertype for types UInt8, String because some of them are String/FixedString and some of them are not
```

## 枚举类型

枚举类型通常在定义常量时使用，ClickHouse提供了Enum8和Enum16两种枚举类型。

```
-- 建表
CREATE TABLE t_enum
(
    x Enum8('hello' = 1, 'world' = 2)
)
ENGINE = TinyLog;
-- INSERT数据
INSERT INTO t_enum VALUES ('hello'), ('world'), ('hello');
-- 如果定义了枚举类型值之后，不能写入其他值的数据
INSERT INTO t_enum values('a')
-- 报异常：Unknown element 'a' for type Enum8('hello' = 1, 'world' = 2)
```

## Tuple类型

Tuple(T1, T2, ...)，元组，与Array不同的是，Tuple中每个元素都有单独的类型，不能在表中存储元组（除了内存表）。它们可以用于临时列分组。在查询中，IN表达式和带特定参数的 lambda 函数可以来对临时列进行分组。

```
SELECT tuple(1,'a') AS x, toTypeName(x);
--结果输出
┌─x───────┬─toTypeName(tuple(1, 'a'))─┐
│ (1,'a') │ Tuple(UInt8, String)      │
└─────────┴───────────────────────────┘
-- 建表
CREATE TABLE t_tuple(
   c1 Tuple(String,Int8)
) engine=TinyLog;
-- INSERT数据
INSERT INTO t_tuple VALUES(('jack',20));
--查询数据
SELECT * FROM t_tuple;
┌─c1──────────┐
│ ('jack',20) │
└─────────────┘
-- 如果插入数据类型不匹配，会报异常
INSERT INTO t_tuple VALUES(('tom','20'));
-- Type mismatch in IN or VALUES section. Expected: Int8. Got: String
```

## 特殊数据类型

### Nullable

Nullable类型表示某个基础数据类型可以是Null值。其具体用法如下所示：

```
-- 建表
CREATE TABLE t_null(x Int8, y Nullable(Int8)) ENGINE TinyLog
-- 写入数据
INSERT INTO t_null VALUES (1, NULL), (2, 3);
SELECT x + y FROM t_null;
-- 结果
┌─plus(x, y)─┐
│       ᴺᵁᴸᴸ │
│          5 │
└────────────┘
```

### Domain

Domain类型是特定实现的类型：

IPv4是与UInt32类型保持二进制兼容的Domain类型，用于存储IPv4地址的值。它提供了更为紧凑的二进制存储的同时支持识别可读性更加友好的输入输出格式。

IPv6是与FixedString(16)类型保持二进制兼容的Domain类型，用于存储IPv6地址的值。它提供了更为紧凑的二进制存储的同时支持识别可读性更加友好的输入输出格式。

注意低版本的ClickHouse不支持此类型。

```
-- 建表
CREATE TABLE hits
(url String, 
 from IPv4
) ENGINE = MergeTree()
ORDER BY from;
-- 写入数据
INSERT INTO hits (url, from) VALUES
('https://wikipedia.org', '116.253.40.133')
('https://clickhouse.tech', '183.247.232.58');

-- 查询
SELECT * FROM hits;
┌─url─────────────────────┬───────────from─┐
│ https://wikipedia.org   │ 116.253.40.133 │
│ https://clickhouse.tech │ 183.247.232.58 │
└─────────────────────────┴────────────────┘
```