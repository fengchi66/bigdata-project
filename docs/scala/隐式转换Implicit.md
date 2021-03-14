# 隐式转换简介

隐式转换是Scala中一种非常有特色的功能，是其他编程语言所不具有的，它允许你手动指定，将某种类型对象转换为另一种类型对象。通过这个功能可是实现 更加灵活强大的功能。

Scala的隐式转换最为核心的就是定义隐式函数，即implicit conversion function，定了隐式函数，在程序的一定作用域中scala会 自动调用。**`Scala会根据隐式函数的签名，在程序中使用到隐式函数参数定义的类型对象时，会自动调用隐式函数将其传入隐式函数，转换为另一种类型对象并返回，这就是scala的“隐式转换”`**。



# 隐式函数

## 转换到一个预期的类型

隐式函数是使用最多的隐式转换，它的主要作用是将一定数据类型对象A转换为另一种类型对象B并返回，使对象A可以使用对象B的属性和函数。

我们来看一个案例，如果在scala中定义一下代码:

```scala
val x:Int=3.5
```

很显然"type mismatch",你不能把一个Double类型赋值到Int类型.

这个时候可以添加一个隐式函数后可以实现Double类型到Int类型的赋值：

```scala
implicit def doubleToInt(a: Double) = a.toInt
```

隐式函数的名称对结构没有影响，即`implicit def doubleToInt(a: Double) = a.toInt`t函数可以是任何名字，只不能采用doubleToInt这种方式函数的意思比较明确，阅读代码的人可以见名知义，增加代码的可读性。

## 扩展类库

**隐式函数的另一个作用就是: 可以快速地扩展现有类库的功能**

需求：通过隐式转化为Int类型增加方法。

```scala
object ExpandLibraryTest {

  // 使用implicit关键字声明的函数称之为隐式函数
  implicit def convert(arg: Int): MyRichInt = MyRichInt(arg)

  def main(args: Array[String]): Unit = {
    println(2.myMax(6))
  }
}

case class MyRichInt(self: Int) {

  def myMax(i: Int): Int = {
    if (self < i) i else self
  }

  def myMin(i: Int): Int = {
    if (self < i) self else i
  }
}
```

以上代码中，为Int类型增加了**myMax**、**myMin**方法，同时将自身作为函数的来使用。

















