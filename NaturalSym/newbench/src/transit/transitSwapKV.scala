package transit

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

/**
  * Created by malig on 3/27/18.
  */
object transitSwapKV extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  // PassengerID, Date, ArriveTime(12:35), DepartureTime(20:06), AirportID
  override def execute(input1: RDD[String]): RDD[String] = {
    input1.map { s =>
      def getDiff(arr: String, dep: String): Int = {
        val a_min = Integer.parseInt(arr.substring(3, 5))
        val a_hr = Integer.parseInt(arr.substring(0, 2))
        val d_min = Integer.parseInt(dep.substring(3, 5))
        val d_hr = Integer.parseInt(dep.substring(0, 2))

        val arr_min = a_hr * 60 + a_min
        val dep_min = d_hr * 60 + d_min


        if (dep_min - arr_min < 0) {
          return 24 * 60 + dep_min - arr_min
        }
        return dep_min - arr_min
      }

      val tokens = s.split(",")
      val arrival_hr = tokens(2).substring(0, 2)
      val diff = getDiff(tokens(3), tokens(2)) // SwapKV
      val airport = tokens(4)
      (airport + arrival_hr, diff)
    }.filter { v =>
      val t1 = v._1
      val t2 = v._2
      t2 < 45
    }.reduceByKey(_ + _).map(m=> m._1 +","+m._2)
  }
}