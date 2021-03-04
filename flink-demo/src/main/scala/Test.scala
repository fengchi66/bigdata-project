import org.apache.flink.streaming.api.scala._

object Test {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment. getExecutionEnvironment

    env.fromCollection(List(1,2,3,4,5,6,7,8)).print()

    env.execute("Job")

  }

}
