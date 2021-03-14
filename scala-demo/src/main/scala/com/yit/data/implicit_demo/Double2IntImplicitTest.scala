package com.yit.data.implicit_demo

object Double2IntImplicitTest {

  def main(args: Array[String]): Unit = {
    implicit def doubleToInt(a: Double) = a.toInt

    val a:Int = 1.2

    println(a)
  }



}
