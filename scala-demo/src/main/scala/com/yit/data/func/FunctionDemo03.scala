package com.yit.data.func

object FunctionDemo03 {

  def main(args: Array[String]): Unit = {

    // （1）定义一个函数，函数参数还是一个函数签名；f表示函数名称;(Int,Int)表示输入两个Int参数；Int表示函数返回值
    def f1(f: (Int, Int) => Int): Int = {
      f(2, 4)
    }

    // （2）定义一个函数，参数和返回值类型和f1的输入参数一致
    def add(a: Int, b: Int): Int = a + b

    // （3）将add函数作为参数传递给f1函数，如果能够推断出来不是调用，_可以省略
    println(f1(add))
    println(f1(add _))
    //可以传递匿名函数
    println(f1((a: Int, b: Int) => a + b))
  }

}
