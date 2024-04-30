package Q12
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }
import utils.SparkProgramTemplate
object Q12_M0 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {}
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String]): RDD[_] = {
    val web_sales = input1
    val date_dim = input2
    val item = input3
    val filtered_item = item.filter { row =>
      val category = row.split(",")(12)
      category.equals("Home") || category.equals("Electronics") || category.equals("Shoes")
    }
    filtered_item.take(10).foreach(println)
    val filtered_dd = date_dim.filter { row =>
      val d_date = row.split(",")(2)
      d_date.substring(0, 7).equals("1999-01")
    }
    val map1 = web_sales.map(row => (row.split(",")(2), row))
    val map7 = filtered_item.map(row => (row.split(",")(0), row))
    val join1 = map1.join(map7)
    val map2 = join1.map { row =>
      val ws_row = row._2._1
      val i_row = row._2._2
      (ws_row.split(",")(33), (ws_row, i_row))
    }
    val map8 = filtered_dd.map(row => (row.split(",")(0), row))
    val join2 = map2.join(map8)
    val map3 = join2.map { row =>
      val ws_row = row._2._1._1
      val i_row = row._2._1._2
      val dd_row = row._2._2
      val i_item_id = i_row.split(",")(1)
      val i_item_desc = i_row.split(",")(4)
      val i_category = i_row.split(",")(12)
      val i_class = i_row.split(",")(10)
      val i_current_price = i_row.split(",")(5)
      val ws_ext_sales_price = ws_row.split(",")(22).toInt
      ((i_item_id, i_item_desc, i_category, i_class, i_current_price), ws_ext_sales_price)
    }
    val rbk2 = map3.reduceByKey(_ / _)
    rbk2
  }
}