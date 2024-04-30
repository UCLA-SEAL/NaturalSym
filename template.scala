import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object grades {
  val conf = new SparkConf()
  conf.setMaster("local[*]")
  conf.setAppName("CommuteTime")
  val sc = new SparkContext(conf)
  sc.setLogLevel("ERROR")

  def main(args: Array[String]): Unit = {
  }

  // execute is the method under test
  def execute(input1: RDD[String], input2: RDD[String]): Any = {
    // 1. input table must be RDD[String] (required by the backend symbolic execution engine)
    // 2. names must be input1, input2, etc
  }
}
