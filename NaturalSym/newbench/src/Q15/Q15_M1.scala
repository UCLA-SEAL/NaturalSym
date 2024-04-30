package Q15
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }
import utils.SparkProgramTemplate
object Q15_M1 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {}
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String]): RDD[_] = {
    val catalog_sales = input1
    val customer = input2
    val customer_address = input3
    val date_dim = input4
    val filtered_dd = date_dim.filter { row =>
      val d_qoy = row.split(",")(10)
      val d_year = row.split(",")(6)
      d_qoy.equals("1") && d_year.equals("1999")
    }
    val map1 = catalog_sales.map(row => (row.split(",")(2), row))
    val map2 = customer.map(row => (row.split(",")(0), row))
    val join1 = map1.join(map2)
    join1.take(10).foreach(println)
    val map3 = join1.map { row =>
      val cs_row = row._2._1
      val c_row = row._2._2
      (c_row.split(",")(4), (cs_row, c_row))
    }
    val map4 = customer_address.map(row => (row.split(",")(0), row))
    val join2 = map3.join(map4)
    val filter1 = join2.filter { row =>
      val cs_row = row._2._1._1
      val c_row = row._2._1._2
      val ca_row = row._2._2
      val ca_zip = ca_row.split(",")(9)
      val ca_state = ca_row.split(",")(8)
      val cs_sales_price = cs_row.split(",")(20).toInt
      val ca_zip5 = ca_zip.substring(0, 5)
      val zip_contains = ca_zip5.equals("85669")
      val states_contains = ca_state.equals("CA")
      zip_contains || cs_sales_price > 500 || states_contains
    }
    val map5 = filter1.map { row =>
      val cs_row = row._2._1._1
      val c_row = row._2._1._2
      val ca_row = row._2._2
      (cs_row.split(",")(33), (cs_row, c_row, ca_row))
    }
    val map6 = filtered_dd.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map6)
    val map7 = join3.map { row =>
      val cs_row = row._2._1._1
      val c_row = row._2._1._2
      val ca_row = row._2._1._3
      val dd_row = row._2._2
      val cs_sales_price = cs_row.split(",")(20).toInt
      val ca_zip = ca_row.split(",")(9)
      (ca_zip, cs_sales_price)
    }
    val rbk1 = map7.reduceByKey(_ / _)
    rbk1
  }
}