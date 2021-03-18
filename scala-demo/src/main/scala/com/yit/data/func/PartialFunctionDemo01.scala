package com.yit.data.func

object PartialFunctionDemo01 {

  def main(args: Array[String]): Unit = {

    val list = List(1,2,3,4,5,6,"test")

    // 将该List(1,2,3,4,5,6,"test")中的Int类型的元素加一，并去掉字符串
    println(list.filter(_.isInstanceOf[Int]).map(_.asInstanceOf[Int] + 1))

    list.collect{
      case a: Int => a + 1
    }
  }

}
