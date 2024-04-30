package Q6
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }
import utils.SparkProgramTemplate
object Q6_M1 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {}
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String]): RDD[_] = {
    val customer_address = input1
    val customer = input2
    val store_sales = input3
    val date_dim = input4
    val item = input5
    val filter1 = date_dim.filter {
      row => row.split(",")(6).equals("1") && row.split(",")(8).equals("2001")
    }
    val map1 = filter1.map(row => (row.split(",")(3), row))
    val map2 = customer_address.map(row => (row.split(",")(0), row))
    val map3 = customer.map(row => (row.split(",")(4), row))
    val join1 = map2.join(map3)
    val map4 = join1.map { row =>
      val ca_row = row._2._1
      val c_row = row._2._2
      (c_row.split(",")(0), (ca_row, c_row))
    }
    val map5 = store_sales.map(row => (row.split(",")(2), row))
    val join2 = map4.join(map5)
    val map6 = join2.map { row =>
      val ca_row = row._2._1._1
      val c_row = row._2._1._2
      val ss_row = row._2._2
      (ss_row.split(",")(22), (ca_row, c_row, ss_row))
    }
    val map7 = date_dim.map(row => (row.split(",")(0), row))
    val join3 = map6.join(map7)
    val map8 = join3.map { row =>
      val ca_row = row._2._1._1
      val c_row = row._2._1._2
      val ss_row = row._2._1._3
      val dd_row = row._2._2
      (ss_row.split(",")(1), (ca_row, c_row, ss_row, dd_row))
    }
    val map9 = item.map(row => (row.split(",")(0), row))
    val join4 = map8.join(map9)
    val map10 = join4.map { row =>
      val ca_row = row._2._1._1
      val c_row = row._2._1._2
      val ss_row = row._2._1._3
      val dd_row = row._2._1._4
      val i_row = row._2._2
      (dd_row.split(",")(3), (ca_row, c_row, ss_row, dd_row, i_row))
    }
    val filter2 = map10.join(map1)
    val filter3 = filter2.filter { row =>
      val i_row = row._2._1._5
      val i_current_price = i_row.split(",")(5).toInt
      i_current_price > 100
    }
    val map12 = filter3.map { row =>
      val ca_row = row._2._1._1
      (ca_row.split(",")(8), 1)
    }
    val rbk1 = map12.reduceByKey(_ - _)
    rbk1
  }
}