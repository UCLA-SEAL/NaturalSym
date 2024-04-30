
package movie1
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object movie1SwapKV extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  // find highly-rated vintage movies earlier than 1960, scored >= 4
  override def execute(input1: RDD[String]): RDD[String] = {
    input1
    .filter(m => {
      val year = m.split(",")(1).toInt
      year > 1900 && year < 1960
    })
    .filter(m => {
      val rating = m.split(",")(3).toInt
      rating >= 4
    })
    .map(m => {
      val genre = m.split(",")(2)
      (1, genre) // swap K, V
    })
    .reduceByKey(_ + _)
    .map(m => m._1 + ":" + m._2)
  }
}
