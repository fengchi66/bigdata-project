package com.yit.data.func

object FunctionDemo01 extends App {

  val aa = "AbC"

  val f1 = (a: String) => a.toLowerCase

  val f2 = new Function[String, String] {
    override def apply(v1: String): String = v1.toLowerCase
  }

  val f3 = new (String, String) {
    def apply(s: String): String = s.toLowerCase


    println(f1(aa))


  }
}

