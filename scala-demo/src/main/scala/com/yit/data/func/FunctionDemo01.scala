package com.yit.data.func

object FunctionDemo01 extends App {

  val aa = "AbC"

  /**
   * 函数值
   */
  val f1 = (a: String) => a.toLowerCase

  val f2 = new Function[String, String] {
    override def apply(v1: String): String = v1.toLowerCase
  }

  val f3 = new (String => String) {
    def apply(s: String): String = s.toLowerCase
  }

  val lower: String => String = _.toLowerCase

  val add: (Int, Int) => Int = _ + _
  // 或者
  def add(a: Int, b: Int): Int = a + b


  println(lower("sdKiFG"))
  println(add(1, 2))
}

