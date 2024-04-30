package Q7
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q7WrongPredicate extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String]): RDD[_] = {

    val customer_demographics = input1
    val promotion = input2
    val store_sales = input3
    val date_dim = input4
    val item = input5

    val filter_cd = customer_demographics.filter {
      row =>
        val cd_gender = row.split(",")(1)
        val cd_marital_status = row.split(",")(2)
        val cd_education_status = row.split(",")(3)

        !cd_gender.equals("M") && cd_marital_status.equals("M") && cd_education_status.equals("Primary")
    }
    // filter_cd.take(10).foreach(println)

    val filtered_p = promotion.filter {
      row =>
        val p_channel_email = row.split(",")(9)
        val p_channel_event = row.split(",")(14)
        p_channel_email.equals("N") && p_channel_event.equals("N")
    }
    // filtered_p.take(10).foreach(println)

    val filtered_dd = date_dim.filter {
      row =>
        val d_year = row.split(",")(6)
        d_year.equals("1999")
    }
    // filtered_p.take(10).foreach(println)

    val map2 = filtered_dd.map(row => (row.split(",")(0)/*d_date_sk*/, row))
    val map1 = store_sales.map(row => (row.split(",")(22) /*ss_sold_date_sk*/, row))
    val join1 = map1.join(map2)
    // join1.take(10).foreach(println)

    val map3 = join1.map {
        row =>
        val ss_row = row._2._1
        val dd_row = row._2._2
        // case (date_sk, (ss_row, dd_row)) =>
          (ss_row.split(",")(1)/*ss_item_sk*/, (ss_row, dd_row))
      }
    val map4 = item.map(row => (row.split(",")(0), row))
    val join2 = map3.join(map4)
    // join2.take(10).foreach(println)

    val map5 = join2.map {
        row =>
        val ss_row = row._2._1._1
        val dd_row = row._2._1._2
        val i_row = row._2._2
        // case (item_sk, ((ss_row, dd_row), i_row)) =>
          (ss_row.split(",")(3)/*ss_cdemo_sk*/, (ss_row, dd_row, i_row))
      }
    val map9 = filter_cd.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map9)
    // join3.take(10).foreach(println)

    val map6 = join3.map {
        row =>
        val ss_row = row._2._1._1
        val dd_row = row._2._1._2
        val i_row = row._2._1._3
        val cd_row = row._2._2
        // case (cdemo_sk, ((ss_row, dd_row, i_row), cd_row)) =>
          (ss_row.split(",")(7)/*ss_promo_sk*/, (ss_row, dd_row, i_row, cd_row))
      }
    val map8 = filtered_p.map(row => (row.split(",")(0), row))
    val join4 = map6.join(map8)
    // join4.take(10).foreach(println)

    val map10 = join4.map {
        row =>
        val ss_row = row._2._1._1
        val dd_row = row._2._1._2
        val i_row = row._2._1._3
        val cd_row = row._2._1._4
        val p_row = row._2._2
        // case (promo_sk, ((ss_row, dd_row, i_row, cd_row), p_row)) =>
          val ss_quantity = ss_row.split(",")(9).toInt // convertColToFloat(ss_row, 9)
          // val ss_list_price = ss_row.split(",")(11).toInt // convertColToFloat(ss_row, 11)
          // val ss_coupon_amt = ss_row.split(",")(18).toInt // convertColToFloat(ss_row, 18)
          // val ss_sales_price = ss_row.split(",")(12).toInt // convertColToFloat(ss_row, 12)

          // (i_row.split(",")(1)/*i_item_id*/, (ss_quantity, ss_list_price, ss_coupon_amt, ss_sales_price, 1))
          (i_row.split(",")(1)/*i_item_id*/, ss_quantity)
      }
      /*
    val rbk1 = map10.reduceByKey {
        case ((a1, a2, a3, a4, count1), (b1, b2, b3, b4, count2)) =>
          (a1+b1, a2+b2, a3+b3, a4+b4, count1+count2)
      }
    rbk1.take(10).foreach(println)*/
    val rbk1 = map10.reduceByKey(_+_)
    rbk1
    /*
    val map7 = rbk1.map {
        case (i_item_id, (sum1, sum2, sum3, sum4, count)) =>
          (i_item_id, sum1/count, sum2/count, sum3/count, sum4/count)
      }
    map7.take(10).foreach(println)

    val sortBy1 = map7.sortBy(_._1)

    sortBy1.take(10).foreach(println)
    */
  }
}