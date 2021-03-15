package com.yit.data.implicit_demo

case class IntWrapper(i: Int) {

  private[IntWrapper] var l = List(i)

  def map(implicit m: Int => List[Int]): List[List[Int]] = {
    l.map(m)
  }
}

object IntWrapper {
  implicit val f =(i : Int) => List(i + 1)
  def main(args : Array[String]) :Unit = {
    //map使用隐式参数，编译器在当前可见作用域中查找到 f 作为map的参数
    print(IntWrapper(3).map) // List(List(2))
  }
}
