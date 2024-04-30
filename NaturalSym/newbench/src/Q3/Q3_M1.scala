package Q3
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }
import utils.SparkProgramTemplate
object Q3_M1 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {}
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String]): RDD[_] = {
    val store_sales = input1
    val date_dim = input2
    val item = input3
    val map1 = store_sales.map(row => (row.split(",")(22), row))
    val filter1 = date_dim.filter(row => row.split(",")(8).equals("11"))
    val map2 = filter1.map(row => (row.split(",")(0), row))
    val join1 = map2.join(map1)
    val map3 = join1.map { row =>
      val date_dim_row = row._2._1
      val ss_row = row._2._2
      (ss_row.split(",")(1), (date_dim_row, ss_row))
    }
    val filter2 = item.filter(row => row.split(",")(13).equals("1"))
    val map4 = filter2.map(row => (row.split(",")(0), row))
    val join2 = map3.join(map4)
    val map5 = join2.map { row =>
      val date_dim_row = row._2._1._1
      val ss_row = row._2._1._2
      val item_row = row._2._2
      val ss_ext_sales_price = ss_row.split(",")(14).toInt
      val ss_sales_price = ss_row.split(",")(12).toInt
      val ss_ext_discount_amt = ss_row.split(",")(13).toInt
      val ss_net_profit = ss_row.split(",")(21).toInt
      val sum = (ss_net_profit + ss_sales_price) * ss_ext_sales_price + ss_ext_discount_amt
      val d_year = date_dim_row.split(",")(6)
      val i_brand = item_row.split(",")(8)
      val i_brand_id = item_row.split(",")(7)
      ((d_year, i_brand, i_brand_id), sum)
    }
    val rbk1 = map5.reduceByKey(_ + _)
    rbk1
  }
}