package credit
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object credit extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  // Analyze the maximum installment payment amount made by each profession for purchasing a new car.
  override def execute(input1: RDD[String]): RDD[String] = {
    //duration_months, credit_amount, purpose, job
    input1.filter(data => {
        val purpose = data.split(",")(2)
        purpose.equals("new car")
    })
    .map(data => {
        val credit_amount = data.split(",")(1).toInt
        val job = data.split(",")(3)
        (job, credit_amount)
    })
    .reduceByKey((x, y) => {
      if (x > y) x else y
    })
    .filter(row => row._2 > 1500)
    .map(x => x._1 + ":" + x._2)
  }
}
