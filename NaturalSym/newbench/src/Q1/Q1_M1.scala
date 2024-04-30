package Q1
import org.apache.spark.rdd.RDD
import org.apache.spark.{ SparkConf, SparkContext }
import utils.SparkProgramTemplate
object Q1_M1 extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {}
  override def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String]): RDD[_] = {
    val YEAR = 1999
    val STATE = "TN"
    val store_returns = input1
    val date_dim = input2
    val store = input3
    val customer = input4
    val filter1 = date_dim.filter(row => row.split(",")(6).equals("1999"))
    println("filter1")
    filter1.foreach(println)
    val map1 = filter1.map(row => (row.split(",")(0), row))
    println("map1")
    map1.foreach(println)
    val map2 = store_returns.map(row => (row.split(",")(19), row))
    println("map2")
    map2.foreach(println)
    val join1 = map2.join(map1)
    println("join1")
    join1.foreach(println)
    val map3 = join1.map { row =>
      val store_returns_row = row._2._1
      val sr_customer_sk = store_returns_row.split(",")(2)
      val sr_store_sk = store_returns_row.split(",")(6)
      val sum_agg_field = store_returns_row.split(",")(10).toInt
      ((sr_customer_sk, sr_store_sk), sum_agg_field)
    }
    println("map3")
    map3.foreach(println)
    val rbk1_0 = map3.reduceByKey(_ + _)
    println("rbk1_0")
    rbk1_0.foreach(println)
    val rbk1 = rbk1_0.filter { row =>
      val sr_store_sk = row._1._2
      val sum_agg_field = row._2
      sum_agg_field != 10000
    }
    println("rbk1")
    rbk1.foreach(println)
    val map4 = rbk1.map { row =>
      val sr_customer_sk = row._1._1
      val sr_store_sk = row._1._2
      val sum_agg_field = row._2
      (sr_store_sk, sr_customer_sk)
    }
    println("map4")
    map4.foreach(println)
    val map10 = store.map(row => (row.split(",")(0), row))
    println("map10")
    map10.foreach(println)
    val join2 = map4.join(map10)
    println("join2")
    join2.foreach(println)
    val map5 = join2.map { row =>
      val sr_customer_sk = row._2._1
      val store_row = row._2._2
      (sr_customer_sk, store_row)
    }
    println("map5")
    map5.foreach(println)
    val map9 = customer.map(row => (row.split(",")(0), row))
    val join3 = map5.join(map9)
    println("join3")
    join3.foreach(println)
    val filter2 = join3.filter { row =>
      val store_row = row._2._1
      store_row.split(",")(24).equals("TN")
    }
    println("filter2")
    filter2.foreach(println)
    val map8 = filter2.map { row =>
      val customer_row = row._2._2
      customer_row.split(",")(1)
    }
    println("map8")
    map8.foreach(println)
    map8
  }
}