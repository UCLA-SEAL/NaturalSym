package Q6
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q6SwapKV extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String]): RDD[_] = {
    val customer_address = input1
    val customer = input2
    val store_sales = input3
    val date_dim = input4
    val item = input5


    val filter1 = date_dim.filter { row => row.split(",")(6).equals("1") && row.split(",")(8).equals("2001") }
    // filter1.take(10).foreach(println)

    val map1 = filter1.map(row => (row.split(",")(3) /*d_month_seq*/, row))
    // val distinct = map1.distinct
    // val take1 = distinct.take(1).head


    val map2 = customer_address.map(row => (row.split(",")(0), row))
    val map3 = customer.map(row => (row.split(",")(4) /*c_current_addr_sk*/ , row))
    val join1 = map2.join(map3)
    // join1.take(10).foreach(println)

    val map4 = join1.map {
      row =>
      // case (addr_sk, (ca_row, c_row)) =>
        val ca_row = row._2._1
        val c_row = row._2._2
        (c_row.split(",")(0) /*c_customer_sk*/ , (ca_row, c_row))
    }
    val map5 = store_sales.map(row => (row.split(",")(2) /*ss_customer_sk*/ , row))
    val join2 = map4.join(map5)
    // join2.take(10).foreach(println)

    val map6 = join2.map {
      row =>
      // case (customer_sk, ((ca_row, c_row), ss_row)) =>
        val ca_row = row._2._1._1
        val c_row = row._2._1._2
        val ss_row = row._2._2
        (ss_row.split(",")(22) /*ss_sold_date_sk*/ , (ca_row, c_row, ss_row))
    }
    val map7 = date_dim.map(row => (row.split(",")(0) /*d_date_sk*/ , row))
    val join3 = map6.join(map7)
    // join3.take(10).foreach(println)

    val map8 = join3.map {
      row =>
      // case (date_sk, ((ca_row, c_row, ss_row), dd_row)) =>
        val ca_row = row._2._1._1
        val c_row = row._2._1._2
        val ss_row = row._2._1._3
        val dd_row = row._2._2
        (ss_row.split(",")(1) /*ss_item_sk*/ , (ca_row, c_row, ss_row, dd_row))
    }
    val map9 = item.map(row => (row.split(",")(0) /*i_item_sk*/ , row))
    val join4 = map8.join(map9)
    // join4.take(10).foreach(println)

    val map10 = join4.map {
      row =>
      // case (item_sk, ((ca_row, c_row, ss_row, dd_row), i_row)) =>
        val ca_row = row._2._1._1
        val c_row = row._2._1._2
        val ss_row = row._2._1._3
        val dd_row = row._2._1._4
        val i_row = row._2._2
        // (ca_row, c_row, ss_row, dd_row, i_row)
        (dd_row.split(",")(3), (ca_row, c_row, ss_row, i_row, dd_row)) // swapKV, i_row<->dd_row
    }

    /*
    // Not sure if this should be applied to the original item table or the partial result
    // confirm with someone
    val map11 = map10
      .map {
        case (_, _, _, _, i_row) =>
          (convertColToFloat(i_row, 5), 1) // j.i_current_price in sql query
      }
    val reduce1 = map11.reduce {
      case ((v1, c1), (v2, c2)) =>
        (v1 + v2, c1 + c2)
    }
    println(s"reduce1 = $reduce1")

    val subquery2_result = reduce1._1 / reduce1._2
    */


    // val filter2 = map10.filter(tup => tup._4.split(",")(3) /*d_month_seq*/ == take1)
    val filter2 = map10
        .join(map1)
    // filter2.take(10).foreach(println)

    val filter3 = filter2.filter {
      // case (_, _, _, _, i_row) =>
      row =>
        val i_row = row._2._1._5
        val i_current_price = i_row.split(",")(5).toInt
        i_current_price > 100 // 1.2 * subquery2_result
    }
    // filter3.take(10).foreach(println)

    val map12 = filter3.map {
      // case (ca_row, c_row, ss_row, dd_row, i_row) =>

      row =>
        val ca_row = row._2._1._1
        (ca_row.split(",")(8), 1) // Took some liberty here, ca_row.split(",")(8) fails due to array out of bounds
    }
    val rbk1 = map12.reduceByKey(_ + _)
    // rbk1.take(10).foreach(println)

    rbk1

    /*
    val filter4 = rbk1.filter {
      // case (state, count) =>
      //  count > 10
      row._2 > 10
    }
    // filter4.take(10).foreach(println)

    val sortBy1 = filter4.sortBy(_._2)
    val take2 = sortBy1.take(10)
    val sortWith1 = take2.sortWith { case (a, b) => (a._2 < b._2) || (a._2 == b._2 && a._1 < b._1) }

    sortWith1.foreach {
      case (state, count) => println(state, count)
    }
    */






    /*
    define YEAR = random(1998, 2002, uniform);
    define MONTH= random(1,7,uniform);
    define _LIMIT=100;

    [_LIMITA] select [_LIMITB] a.ca_state state, count(*) cnt
    from customer_address a
        ,customer c
        ,store_sales s
        ,date_dim d
        ,item i
    where       a.ca_address_sk = c.c_current_addr_sk
      and c.c_customer_sk = s.ss_customer_sk
      and s.ss_sold_date_sk = d.d_date_sk
      and s.ss_item_sk = i.i_item_sk
      and d.d_month_seq =
           (select distinct (d_month_seq)
            from date_dim
                  where d_year = [YEAR]
              and d_moy = [MONTH] )
      and i.i_current_price > 1.2 *
                (select avg(j.i_current_price)
           from item j
           where j.i_category = i.i_category)
    group by a.ca_state
    having count(*) >= 10
    order by cnt, a.ca_state
    [_LIMITC];


    */

  }
}