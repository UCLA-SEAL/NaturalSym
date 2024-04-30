package Q1
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object Q1SwapKV extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String]): RDD[_] = {
    val YEAR = 1999 // rand.nextInt(2002 - 1998) + 1998
    val STATE = "TN"

    val store_returns = input1
    val date_dim = input2
    val store = input3
    val customer = input4

    val filter1 = date_dim.filter(row => row.split(",")(6).equals("1999"))

    // filter1.take(10).foreach(println)
    println("filter1")
    filter1.foreach(println)


    val map1 = filter1.map(row => (row.split(",")(0), row))
    println("map1")
    map1.foreach(println)

    // map1.take(10).foreach(println)

    val map2 = store_returns.map(row => (row.split(",")(19)/*last*/, row))
    println("map2")
    map2.foreach(println)

    // map2.take(10).foreach(println)

    val join1 = map2.join(map1)
    println("join1")
    join1.foreach(println)

    // join1.take(10).foreach(println)

    val map3 = join1.map { row =>
      val store_returns_row = row._2._1
      val sr_customer_sk = store_returns_row.split(",")(2)
      val sr_store_sk = store_returns_row.split(",")(6)
      // sum([AGG_FIELD])
      // val sum_agg_field = List(10, 11, 12, 13, 15, 16, 17).map(n => convertColToFloat(store_returns_row, n)).reduce(_ + _)
      val sum_agg_field = store_returns_row.split(",")(10).toInt // + store_returns_row.split(",")(11).toInt + store_returns_row.split(",")(12).toInt + store_returns_row.split(",")(13).toInt + store_returns_row.split(",")(14).toInt +  store_returns_row.split(",")(15).toInt +  store_returns_row.split(",")(16).toInt + store_returns_row.split(",")(17).toInt;
      ((sr_store_sk, sr_customer_sk), sum_agg_field) // SwapKV: sr_customer_sk, sr_store_sk
    }
    println("map3")
    map3.foreach(println)

    // map3.take(10).foreach(println)

    val rbk1_0 = map3.reduceByKey(_+_)
    println("rbk1_0")
    rbk1_0.foreach(println)
    val rbk1 = rbk1_0
      .filter{
        row =>
          val sr_store_sk = row._1._2
          val sum_agg_field = row._2
          sum_agg_field > 10000
      }

    println("rbk1")
    rbk1.foreach(println)
    // println("rbk1")
    // rbk1.take(10).foreach(println)

    val map4 = rbk1.map {
      row =>
        // case ((sr_customer_sk, sr_store_sk), rest) => (sr_store_sk, rest)
        val sr_customer_sk = row._1._1
        val sr_store_sk = row._1._2
        val sum_agg_field = row._2
        (sr_store_sk, sr_customer_sk) //(sr_customer_sk, sr_store_sk, sum_agg_field)
    }
    println("map4")
    map4.foreach(println)

    // map4.take(10).foreach(println)

    val map10 = store.map(row => (row.split(",")(0), row))
    println("map10")
    map10.foreach(println)
    val join2 = map4.join(map10)
    println("join2")
    join2.foreach(println)

    //println("join2")
    //join2.take(10).foreach(println)

    val map5 = join2.map {
      row => 
        val sr_customer_sk = row._2._1
        val store_row = row._2._2
      // case (store_sk, (ctr_row@(sr_customer_sk, st_store_sk, sum_agg_field), store_row)) => (sr_customer_sk, (ctr_row, store_row))
        (sr_customer_sk, store_row)
    }
    println("map5")
    map5.foreach(println)
    //map5.take(10).foreach(println)

    val map9 = customer.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map9)
    println("join3")
    join3.foreach(println)

   //  join3.take(10).foreach(println)
   /*
    val map6 = join3.map {
      row =>
        val sum_agg_field = row._2._1._1
        val store_row = row._2._1._2
        val customer_row = row._2._2
      // case (customer_sk, ((ctr_row, store_row), customer_row)) => (ctr_row, store_row, customer_row)
        (row._2._1._1, row._2._1._2, row._2._2)
    }*/

    //map6.take(10).foreach(println)
    /*
    val map7 = map6.map {
      case ((_, _, total_return), _, _) => (total_return, 1)
    }

    map7.take(10).foreach(println)

    val reduce1 = map7.reduce { case ((v1, c1), (v2, c2)) => (v1 + v2, c1 + c2) }

    val avg = reduce1._1 / reduce1._2.toFloat * 1.2f
    */

    // ---------------------------------------------------------------------------------------
    val filter2 = join3.filter {
      row =>
      // case ((_, _, return_total), store_row, _) =>
        val store_row = row._2._1
        //          _root_.monitoring.Monitors.monitorPredicateSymEx(
        //            return_total > avg && store_row(24) == STATE,
        //            (List(return_total, avg, store_row(24),STATE), List()),
        //            1,
        //            expressionAccumulator)
        store_row.split(",")(24).equals("TN")
    }
    println("filter2")
    filter2.foreach(println)

    //filter2.take(10).foreach(println)

    val map8 = filter2
      .map {
      // case (_, ((return_total, store_row), customer_row))
        row =>
          val customer_row = row._2._2
        // case (_, _, customer_row) => customer_row.split(",")(1)
        customer_row.split(",")(1)
      }
    // map8.take(10).foreach(println)
    
    println("map8")
    map8.foreach(println)
    map8
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