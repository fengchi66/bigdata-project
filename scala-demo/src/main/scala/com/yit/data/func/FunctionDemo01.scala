package com.yit.data.func

object FunctionDemo01 extends App {

  //  val aa = "AbC"
  //
  //  /**
  //   * 函数值
  //   */
  //  val f1 = (a: String) => a.toLowerCase
  //
  //  val f2 = new Function[String, String] {
  //    override def apply(v1: String): String = v1.toLowerCase
  //  }
  //
  //  val f3 = new (String => String) {
  //    def apply(s: String): String = s.toLowerCase
  //  }
  //
  //  val lower: String => String = _.toLowerCase
  //
  ////  val add: (Int, Int) => Int = _ + _
  //  // 或者
  //  def add(a: Int, b: Int): Int = a + b
  //
  //  val salaries = Seq(20000, 70000, 40000)
  //  val newSalaries = salaries.map(x => x * 2) // List(40000, 140000, 80000)
  //
  //  def f4(f: (Int, Int) => Int): Int = {
  //    f(2, 4)
  //  }

  def f1() = {
    def f2() = {}
    f2 _
  }

  val f = f1()
  // 因为f1函数的返回值依然为函数，所以可以变量f可以作为函数继续调用
  f()
  // 上面的代码可以简化为
  f1()()

  // println(f4(add))


//  println(lower("sdKiFG"))
//  println(add(1, 2))
}

