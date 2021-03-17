import org.apache.flink.streaming.api.scala._

object Test {
  def main(args: Array[String]): Unit = {

<<<<<<< HEAD
    val env = StreamExecutionEnvironment. getExecutionEnvironment

    env.fromCollection(List(1,2,3,4,5,6,7,8)).print()

    env.execute("Job")
=======
    env.addSource(new Source).assignAscendingTimestamps(_.ts)

>>>>>>> 4aabcb9544e0851d3ac608f848641a3fd6a6b60f

  }

}
