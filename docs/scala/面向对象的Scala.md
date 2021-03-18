# 类和对象

Classes就是类，和java中的类相似，它里面可以包含方法、常量、变量、类型、对象、特质、类等。

一个最简的类的定义就是关键字class+标识符，类名首字母应大写。如下所示：

```scala
class Family

val family = new Family
```

new关键字是用来创建类的实例。在上面的例子中，Family没有定义构造器，所以默认带有一个无参的默认的构造器。

 

- 构造器

那么怎么给类加一个构造器呢？

```scala
class Point(var x: Int, var y: Int) {

  override def toString: String =
    s"($x, $y)"
}

val point1 = new Point(2, 3)
point1.x  // 2
println(point1)  // prints (2, 3)
```

和其他的编程语言不同的是，Scala的类构造器定义在类的签名中：(var x: Int, var y: Int)。 这里我们还重写了AnyRef里面的toString方法。

构造器也可以拥有默认值：

```scala
class Point(var x: Int = 0, var y: Int = 0)

val origin = new Point  // x and y are both set to 0
val point1 = new Point(1)
println(point1.x)  // prints 1
```

主构造方法中带有val和var的参数是公有的。然而由于val是不可变的，所以不能像下面这样去使用。

```scala
class Point(val x: Int, val y: Int)
val point = new Point(1, 2)
point.x = 3  // <-- does not compile
```

不带val或var的参数是私有的，仅在类中可见。

```scala
class Point(x: Int, y: Int)
val point = new Point(1, 2)
point.x  // <-- does not compile
```

- 私有成员和Getter/Setter语法

Scala的成员默认是public的。如果想让其变成私有的，可以加上private修饰符，Scala的getter和setter语法和java不太一样，下面我们来举个例子：

```scala
class Point {

  private var _x = 0
  private var _y = 0
  private val bound = 100

  def x = _x
  def x_= (newValue: Int): Unit = {
    if (newValue < bound) _x = newValue else printWarning
  }

  def y = _y
  def y_= (newValue: Int): Unit = {
    if (newValue < bound) _y = newValue else printWarning
  }

  private def printWarning = println("WARNING: Out of bounds")
}

object Point {
  def main(args: Array[String]): Unit = {
    val point1 = new Point
    point1.x = 99
    point1.y = 101 // prints the warning
  }
}
```

我们定义了两个私有变量_x, _y, 同时还定义了他们的get方法x和y，那么相应的他们的set方法就是x_ 和y_, 在get方法后面加上下划线就可以了。

