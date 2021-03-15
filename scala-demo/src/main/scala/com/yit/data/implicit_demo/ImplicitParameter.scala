package com.yit.data.implicit_demo

object ImplicitParameter extends App {
  def compare[T](first:T, second:T)(implicit order:T => Ordered[T]): T = {
    if (first < second) first else second
  }
  println(compare("A","B"))
}
