package Q7
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }
import utils.SparkProgramTemplate
object Q7_M0 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {}
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String]): RDD[_] = {
    val customer_demographics = input1
    val promotion = input2
    val store_sales = input3
    val date_dim = input4
    val item = input5
    val filter_cd = customer_demographics.filter { row =>
      val cd_gender = row.split(",")(1)
      val cd_marital_status = row.split(",")(2)
      val cd_education_status = row.split(",")(3)
      cd_gender.equals("M") && cd_marital_status.equals("M") && cd_education_status.equals("Primary")
    }
    val filtered_p = promotion.filter { row =>
      val p_channel_email = row.split(",")(9)
      val p_channel_event = row.split(",")(14)
      p_channel_email.equals("N") && p_channel_event.equals("N")
    }
    val filtered_dd = date_dim.filter { row =>
      val d_year = row.split(",")(6)
      d_year.equals("1999")
    }
    val map2 = filtered_dd.map(row => (row.split(",")(0), row))
    val map1 = store_sales.map(row => (row.split(",")(22), row))
    val join1 = map1.join(map2)
    val map3 = join1.map { row =>
      val ss_row = row._2._1
      val dd_row = row._2._2
      (ss_row.split(",")(1), (ss_row, dd_row))
    }
    val map4 = item.map(row => (row.split(",")(0), row))
    val join2 = map3.join(map4)
    val map5 = join2.map { row =>
      val ss_row = row._2._1._1
      val dd_row = row._2._1._2
      val i_row = row._2._2
      (ss_row.split(",")(3), (ss_row, dd_row, i_row))
    }
    val map9 = filter_cd.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map9)
    val map6 = join3.map { row =>
      val ss_row = row._2._1._1
      val dd_row = row._2._1._2
      val i_row = row._2._1._3
      val cd_row = row._2._2
      (ss_row.split(",")(7), (ss_row, dd_row, i_row, cd_row))
    }
    val map8 = filtered_p.map(row => (row.split(",")(0), row))
    val join4 = map6.join(map8)
    val map10 = join4.map { row =>
      val ss_row = row._2._1._1
      val dd_row = row._2._1._2
      val i_row = row._2._1._3
      val cd_row = row._2._1._4
      val p_row = row._2._2
      val ss_quantity = ss_row.split(",")(9).toInt
      (i_row.split(",")(1), ss_quantity)
    }
    val rbk1 = map10.reduceByKey(_ - _)
    rbk1
  }
}