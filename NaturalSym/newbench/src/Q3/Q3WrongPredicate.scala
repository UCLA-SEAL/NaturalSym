package Q3
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q3WrongPredicate extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String]): RDD[_] = {
    val store_sales = input1
    val date_dim = input2
    val item = input3

    val map1 = store_sales.map(row => (row.split(",")(22), row))

    // map1.take(10).foreach(println)

    val filter1 = date_dim.filter(row => !row.split(",")(8).equals("11") /*d_moy*/) // Wrong predicate

    val map2 = filter1.map(row => (row.split(",")(0), row))

    // map2.take(10).foreach(println)
    // t.d_date_sk = store_sales.ss_sold_date_sk
    val join1 = map2.join(map1)

    // join1.take(10).foreach(println)

    val map3 = join1.map {
      row =>
        val date_dim_row = row._2._1
        val ss_row = row._2._2
      //case (date_sk, (date_dim_row, ss_row)) =>
        (ss_row.split(",")(1) /*ss_item_sk*/ , (date_dim_row, ss_row))
    }
    // and store_sales.ss_item_sk = item.i_item_sk

    val filter2 = item.filter(row => row.split(",")(13).equals("1") /*i_manufact_id*/) // and item.i_manufact_id = [MANUFACT]
    val map4 = filter2.map(row => (row.split(",")(0), row))

    val join2 = map3.join(map4)

    val map5 = join2.map {
      row =>
        val date_dim_row = row._2._1._1
        val ss_row = row._2._1._2
        val item_row = row._2._2

      // case (item_sk, ((date_dim_row, ss_row), item_row)) =>
        val ss_ext_sales_price = ss_row.split(",")(14).toInt //convertColToFloat(ss_row, 14)
        val ss_sales_price = ss_row.split(",")(12).toInt // convertColToFloat(ss_row, 12)
        val ss_ext_discount_amt = ss_row.split(",")(13).toInt //convertColToFloat(ss_row, 13)
        val ss_net_profit = ss_row.split(",")(21).toInt // convertColToFloat(ss_row, 21)
        val sum = ss_net_profit + ss_sales_price + ss_ext_sales_price + ss_ext_discount_amt

        val d_year = date_dim_row.split(",")(6)
        val i_brand = item_row.split(",")(8)
        val i_brand_id = item_row.split(",")(7)

        // ((d_year, i_brand, i_brand_id), (d_year, i_brand, i_brand_id, sum))
        ((d_year, i_brand, i_brand_id), sum)
    }

    val rbk1 = map5.reduceByKey(_+_)
    rbk1



    /*
    define AGGC= text({"ss_ext_sales_price",1},{"ss_sales_price",1},{"ss_ext_discount_amt",1},{"ss_net_profit",1});
    define MONTH = random(11,12,uniform);
    define MANUFACT= random(1,1000,uniform);
    define _LIMIT=100;

    [_LIMITA] select [_LIMITB] dt.d_year
          ,item.i_brand_id brand_id
          ,item.i_brand brand
          ,sum([AGGC]) sum_agg
    from  date_dim dt
         ,store_sales
         ,item
    where dt.d_date_sk = store_sales.ss_sold_date_sk
      and store_sales.ss_item_sk = item.i_item_sk
      and item.i_manufact_id = [MANUFACT]
      and dt.d_moy=[MONTH]
    group by dt.d_year
         ,item.i_brand
         ,item.i_brand_id
    order by dt.d_year
            ,sum_agg desc
            ,brand_id
    [_LIMITC];

    */

  }
}