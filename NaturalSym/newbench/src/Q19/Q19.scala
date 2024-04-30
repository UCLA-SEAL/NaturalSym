package Q19
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q19 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String], input6: RDD[String]): RDD[_] = {
    val date_dim = input1
    val store_sales = input2
    val item = input3
    val customer = input4
    val customer_address = input5
    val store = input6

    val filtered_i = item
      .filter {
        row =>
          val i_manager_id = row.split(",")(20)
          i_manager_id.equals("50")
      }

    val filtered_dd = date_dim
      .filter {
        row =>
          val d_moy = row.split(",")(8)
          val d_year = row.split(",")(6)
          d_moy.equals("11") && d_year.equals("1999")
      }

    val map1 = filtered_dd.map(row => (row.split(",")(0) /*.head*/, row))
    val map2 = store_sales.map(row => (row.split(",")(22) /*ss_sold_date_sk, last*/, row))
    val join1 = map1.join(map2)
    // join1.take(10).foreach(println)

    val map3 = join1.map {
      row =>
        // case (_, (dd_row, ss_row)) =>
          val dd_row = row._2._1
          val ss_row = row._2._2
          (ss_row.split(",")(1) /*ss_item_sk*/, (dd_row, ss_row))
      }
    val map4 = filtered_i.map(row => (row.split(",")(0) /*.head*/, row))
    val join2 = map3.join(map4)

    val map5 = join2.map {
      row =>
        // case (_, ((dd_row, ss_row), i_row)) =>
          val dd_row = row._2._1._1
          val ss_row = row._2._1._2
          val i_row = row._2._2
          (ss_row.split(",")(2)/*ss_customer_sk*/, (dd_row, ss_row, i_row))
      }
    val map6 = customer.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map6)

    val map7 = join3.map {
      row =>
        // case (_, ((dd_row, ss_row, i_row), c_row)) =>
          val dd_row = row._2._1._1
          val ss_row = row._2._1._2
          val i_row = row._2._1._3
          val c_row = row._2._2

          (c_row.split(",")(4)/*c_current_addr_sk*/, (dd_row, ss_row, i_row, c_row))
      }
    val map8 = customer_address.map(row => (row.split(",")(0), row))
    val join4 = map7.join(map8)

    val map9 = join4.map {
      row =>
          val dd_row = row._2._1._1
          val ss_row = row._2._1._2
          val i_row = row._2._1._3
          val c_row = row._2._1._4
          val ca_row = row._2._2
        // case (_, ((dd_row, ss_row, i_row, c_row), ca_row)) =>
          (ss_row.split(",")(6)/*ss_store_sk*/, (dd_row, ss_row, i_row, c_row, ca_row))
      }
    val map10 = store.map(row => (row.split(",")(0), row))
    val join5 = map9.join(map10)

    val map11 = join5.map {
      row =>
        val dd_row = row._2._1._1
        val ss_row = row._2._1._2
        val i_row = row._2._1._3
        val c_row = row._2._1._4
        val ca_row = row._2._1._5
        val s_row = row._2._2
        // case (_, ((dd_row, ss_row, i_row, c_row, ca_row), s_row)) =>
          // (dd_row, ss_row, i_row, c_row, ca_row, s_row)
          (ss_row, i_row, c_row, ca_row, s_row)
      }
    val filter1 = map11.filter {
      row =>
        val ca_row = row._4
        val s_row = row._5
        // case (_, _, _, _, ca_row, s_row) =>
          val ca_zip = ca_row.split(",")(9)
          val s_zip = s_row.split(",")(25)
          !ca_zip.substring(0, 5).equals(s_zip.substring(0, 5))
      }
    // filter1.take(10).foreach(println)

    val map12 = filter1.map {
      row =>
        val ss_row = row._1
        val i_row = row._2
        // case (_, ss_row, i_row, _, _, _) =>
          val ss_ext_sales_price = ss_row.split(",")(14).toInt
          val i_brand_id = i_row.split(",")(7)
          val i_brand = i_row.split(",")(8)
          val i_manufact_id = i_row.split(",")(13)
          val i_manufact = i_row.split(",")(14)
          ((i_brand_id, i_brand, i_manufact_id, i_manufact), ss_ext_sales_price)
      }
    val rbk1 = map12.reduceByKey(_+_)
    rbk1
  }
  /* ORIGINAL QUERY:
  define COUNTY = random(1, rowcount("active_counties", "store"), uniform);
  define STATE = distmember(fips_county, [COUNTY], 3);
  define YEAR = random(1998, 2002, uniform);
  define AGG_FIELD = text({"SR_RETURN_AMT",1},{"SR_FEE",1},{"SR_REFUNDED_CASH",1},{"SR_RETURN_AMT_INC_TAX",1},{"SR_REVERSED_CHARGE",1},{"SR_STORE_CREDIT",1},{"SR_RETURN_TAX",1});
  define _LIMIT=100;

  with customer_total_return as
  (
      select sr_customer_sk as ctr_customer_sk ,sr_store_sk as ctr_store_sk ,sum([AGG_FIELD])
                                                                                  as ctr_total_return
      from store_returns ,date_dim
      where sr_returned_date_sk = d_date_sk
      and d_year =[YEAR]
      group by sr_customer_sk ,sr_store_sk
  )
  [_LIMITA]

  select [_LIMITB] c_customer_id
  from customer_total_return ctr1 ,store ,customer
  where ctr1.ctr_total_return >   (
                                      -- subquery 1
                                      select avg(ctr_total_return)*1.2
                                      from customer_total_return ctr2
                                      where ctr1.ctr_store_sk = ctr2.ctr_store_sk
                                  )
  and s_store_sk = ctr1.ctr_store_sk
  and s_state = '[STATE]'
  and ctr1.ctr_customer_sk = c_customer_sk
  order by c_customer_id
  [_LIMITC];
   */
}