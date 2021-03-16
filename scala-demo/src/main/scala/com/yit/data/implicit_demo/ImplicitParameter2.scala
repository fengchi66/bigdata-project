package com.yit.data.implicit_demo

object ImplicitParameter2 extends App {

  def compare[T <% Ordered[T]](first:T, second:T)(): T = {
    if (first < second) first else second
  }

}
