package Q20
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q20WrongOffset extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String]): RDD[_] = {
    val catalog_sales = input1 // columns: 34
    val date_dim = input2 // columns: 28
    val item = input3 //columns: 22

    val filtered_item = item.filter {
      row =>
        val category = row.split(",")(12)
        category.equals("Home") || category.equals("Electronics") || category.equals("Shoes")
    }
    val filtered_dd = date_dim.filter {
      row =>
        val d_date = row.split(",")(2)
        d_date.substring(2, 9).equals("1998-01") // Q20WrongOffset: 0, 7
    }

    val main_query_part1 = catalog_sales
      .map(row => (row.split(",")(2)/*ws_item_sk*/, row))
      .join(filtered_item.map(row => (row.split(",")(0), row)))
      .map {
        row =>
          val item_sk = row._1
          val cs_row = row._2._1
          val i_row = row._2._2
          (cs_row.split(",")(33)/*ws_sold_date*/, (cs_row, i_row))
      }
      .join(filtered_dd.map(row => (row.split(",")(0), row)))
      .map {
        row =>
          val cs_row = row._2._1._1
          val i_row = row._2._1._2
          val dd_row = row._2._2
        // case (_, ((cs_row, i_row), dd_row)) =>
          //val i_item_id = i_row.split(",")(1)
          //val i_item_desc = i_row.split(",")(4)
          //val i_category = i_row.split(",")(12)
          //val i_class = i_row.split(",")(10)
          //val i_current_price = i_row.split(",")(5)
          //val cs_ext_sales_price = cs_row.split(",")(22)
          (cs_row, i_row) // there should be another value here
          // (i_class, i_item_id + "," + i_item_desc + "," + i_category + "," + i_current_price, cs_ext_sales_price)
      }
    //main_query_part1.map(row=>row._1+row._2+row._3)
      
    val item_revenues = main_query_part1
      .map{
        row =>
          val cs_row = row._1
          val i_row = row._2
          val i_item_id = i_row.split(",")(1)
          val i_item_desc = i_row.split(",")(4)
          val i_category = i_row.split(",")(12)
          val i_class = i_row.split(",")(10)
          val i_current_price = i_row.split(",")(5)
          val cs_ext_sales_price = cs_row.split(",")(22).toInt
          // (i_item_id + "," + i_item_desc + "," + i_category  + "," + i_current_price + "," + i_class, cs_ext_sales_price)
          ((i_item_id, i_item_desc, i_category, i_class, i_current_price), cs_ext_sales_price)
      }
      .reduceByKey(_ + _)
    // item_revenues.map(row => row._1._1 + "," + row._2)
      /*.map {
        case ((i_item_id, i_item_desc, i_category, i_class, i_current_price), cs_ext_sales_price) =>
          (i_class, (i_item_id, i_item_desc, i_category, i_current_price, cs_ext_sales_price))
      }
      .join(revenue_by_class)
      .map {
        case (i_class, ((i_item_id, i_item_desc, i_category, i_current_price, cs_ext_sales_price), class_rev)) =>
          (i_item_id, i_item_desc, i_category, i_class, i_current_price, cs_ext_sales_price, cs_ext_sales_price/class_rev)
      }*/


    val revenue_by_class = main_query_part1.map {
      row =>
        val cs_row = row._1
        val i_row = row._2
        val i_class = i_row.split(",")(10)
        val cs_ext_sales_price = cs_row.split(",")(22).toInt
        (i_class, cs_ext_sales_price)
      }
      .reduceByKey(_ + _)
    
    item_revenues.map {
      row =>
        val i_class = row._1._4
        (i_class, ((row._1._1: String, row._1._2: String, row._1._3: String, row._1._4: String, row._1._5: String), row._2))
      }
      .join(revenue_by_class)
  }
}