package Q19
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }
import utils.SparkProgramTemplate
object Q19_M0 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {}
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String], input6: RDD[String]): RDD[_] = {
    val date_dim = input1
    val store_sales = input2
    val item = input3
    val customer = input4
    val customer_address = input5
    val store = input6
    val filtered_i = item.filter { row =>
      val i_manager_id = row.split(",")(20)
      i_manager_id.equals("50")
    }
    val filtered_dd = date_dim.filter { row =>
      val d_moy = row.split(",")(8)
      val d_year = row.split(",")(6)
      d_moy.equals("11") && d_year.equals("1999")
    }
    val map1 = filtered_dd.map(row => (row.split(",")(0), row))
    val map2 = store_sales.map(row => (row.split(",")(22), row))
    val join1 = map1.join(map2)
    val map3 = join1.map { row =>
      val dd_row = row._2._1
      val ss_row = row._2._2
      (ss_row.split(",")(1), (dd_row, ss_row))
    }
    val map4 = filtered_i.map(row => (row.split(",")(0), row))
    val join2 = map3.join(map4)
    val map5 = join2.map { row =>
      val dd_row = row._2._1._1
      val ss_row = row._2._1._2
      val i_row = row._2._2
      (ss_row.split(",")(2), (dd_row, ss_row, i_row))
    }
    val map6 = customer.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map6)
    val map7 = join3.map { row =>
      val dd_row = row._2._1._1
      val ss_row = row._2._1._2
      val i_row = row._2._1._3
      val c_row = row._2._2
      (c_row.split(",")(4), (dd_row, ss_row, i_row, c_row))
    }
    val map8 = customer_address.map(row => (row.split(",")(0), row))
    val join4 = map7.join(map8)
    val map9 = join4.map { row =>
      val dd_row = row._2._1._1
      val ss_row = row._2._1._2
      val i_row = row._2._1._3
      val c_row = row._2._1._4
      val ca_row = row._2._2
      (ss_row.split(",")(6), (dd_row, ss_row, i_row, c_row, ca_row))
    }
    val map10 = store.map(row => (row.split(",")(0), row))
    val join5 = map9.join(map10)
    val map11 = join5.map { row =>
      val dd_row = row._2._1._1
      val ss_row = row._2._1._2
      val i_row = row._2._1._3
      val c_row = row._2._1._4
      val ca_row = row._2._1._5
      val s_row = row._2._2
      (ss_row, i_row, c_row, ca_row, s_row)
    }
    val filter1 = map11.filter { row =>
      val ca_row = row._4
      val s_row = row._5
      val ca_zip = ca_row.split(",")(9)
      val s_zip = s_row.split(",")(25)
      !ca_zip.substring(0, 5).equals(s_zip.substring(0, 5))
    }
    val map12 = filter1.map { row =>
      val ss_row = row._1
      val i_row = row._2
      val ss_ext_sales_price = ss_row.split(",")(14).toInt
      val i_brand_id = i_row.split(",")(7)
      val i_brand = i_row.split(",")(8)
      val i_manufact_id = i_row.split(",")(13)
      val i_manufact = i_row.split(",")(14)
      ((i_brand_id, i_brand, i_manufact_id, i_manufact), ss_ext_sales_price)
    }
    val rbk1 = map12.reduceByKey(_ - _)
    rbk1
  }
}