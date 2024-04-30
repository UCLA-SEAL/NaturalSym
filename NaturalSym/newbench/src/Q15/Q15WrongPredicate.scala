package Q15
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q15WrongPredicate extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String]): RDD[_] = {
    // val ZIPS = List("85669","86197","88274","83405","86475","85392","85460","80348","81792")
    // val STATES = List("CA", "WA", "GA")

    val catalog_sales = input1
    val customer = input2
    val customer_address = input3
    val date_dim = input4

    val filtered_dd = date_dim
      .filter {
        row =>
          val d_qoy = row.split(",")(10)
          val d_year = row.split(",")(6)
          d_qoy.equals("1") && d_year.equals("1999")
      }
    // filtered_dd.foreach(println)

    val map1 = catalog_sales.map(row => (row.split(",")(2)/*cs_bill_customer_sk*/, row))
    val map2 = customer.map(row => (row.split(",")(0), row))
    val join1 = map1.join(map2)
    join1.take(10).foreach(println)
    val map3 = join1.map {
        row =>
        val cs_row = row._2._1
        val c_row = row._2._2
        // case (_, (cs_row, c_row)) =>
          (c_row.split(",")(4)/*c_current_addr_sk*/, (cs_row, c_row))
      }
    val map4 = customer_address.map(row => (row.split(",")(0), row))
    val join2 = map3.join(map4)
    // join2.take(10).foreach(println)
    
    val filter1 = join2.filter {
        // case (_, ((cs_row, c_row), ca_row)) =>
        row =>
            val cs_row = row._2._1._1
            val c_row = row._2._1._2
            val ca_row = row._2._2
          val ca_zip = ca_row.split(",")(9) // took liberty here (if the row is malformed for some reason
          val ca_state = ca_row.split(",")(8) // 
          val cs_sales_price = cs_row.split(",")(20).toInt

          // List("CA", "WA", "GA")
          val ca_zip5 = ca_zip.substring(0, 5)
          val zip_contains = ca_zip5.equals("85669") // || ca_zip5.equals("86197") || ca_zip5.equals("88274") || ca_zip5.equals("83405")
          val states_contains = ca_state.equals("CA") // || ca_state.equals("WA") || ca_state.equals("GA")
          // List("85669","86197","88274","83405","86475","85392","85460","80348","81792")
          // (ZIPS.contains(ca_zip.take(5)) || cs_sales_price > 500 || STATES.contains(ca_state))
          (zip_contains && cs_sales_price > 500 && states_contains) //Q15WrongPredicate: || 
      }
    val map5 = filter1.map {
      row =>
        val cs_row = row._2._1._1
        val c_row = row._2._1._2
        val ca_row = row._2._2
    // case (_, ((cs_row, c_row), ca_row)) =>
        (cs_row.split(",")(33)/*cs_sold_date_sk*/, (cs_row, c_row, ca_row))
    }
    // filter1.take(10).foreach(println)

    val map6 = filtered_dd.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map6)
    // join3.take(10).foreach(println)

    val map7 = join3.map {
        // case (_, ((cs_row, c_row, ca_row), dd_row)) =>
        row =>
          val cs_row = row._2._1._1
          val c_row = row._2._1._2
          val ca_row = row._2._1._3
          val dd_row = row._2._2
          val cs_sales_price = cs_row.split(",")(20).toInt
          val ca_zip = ca_row.split(",")(9)
          (ca_zip, cs_sales_price)
      }
    val rbk1 = map7.reduceByKey(_+_)
    rbk1
    // rbk1.take(10).foreach(println)

   //  val sortBy1 = rbk1.sortBy(_._1)

    // sortBy1.take(10).foreach(println)

    
  }
}