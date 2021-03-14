package com.yit.data.implicit_demo

/**
 * 扩展类库
 */
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
