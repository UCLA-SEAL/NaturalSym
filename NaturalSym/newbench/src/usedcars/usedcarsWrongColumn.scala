// https://github.com/saturndatacloud/datasets/blob/master/Automative/Carvana/used%20cars/carvana_used_cars.csv
package usedcars
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object usedcarsWrongColumn extends SparkProgramTemplate {
  // select all the second-hand car information with an age of no more than 10 years and a discount of more than 5000."
  def main(args: Array[String]): Unit = {
  }
//"vehicle_id","stock_number","year","make","model","miles","trim","sold_price","discounted_sold_price","partnered_dealership","delivery_fee","earliest_delivery_date","sold_date"
//"2388462","2001823541","2016","Ford","Focus","77108","SE","14590","14590","False","490","2022-08-11T16:29:53.448Z","2022-08-05"
  override def execute(input1: RDD[String], input2: RDD[String]): RDD[String] = {
    //vehicle_id,model
    //vehicle_id,year,price,discounted,sold-date,miles
    val basics = input1.map(rows => {
        val vehicle_id = rows.split(",")(0)
        val model = rows.split(",")(1)
        (vehicle_id, model)
    })
    val sales = input2.filter(rows => {
        val price = rows.split(",")(2).toInt
        val discounted = rows.split(",")(3).toInt
        price - discounted > 5000
    })
    .filter(rows => {
        val sold_year = rows.split(",")(4).substring(0, 5).toInt // (0, 4)
        val pro_year = rows.split(",")(1).toInt
        sold_year - pro_year < 10
    })
    .map(rows => {
        val vehicle_id = rows.split(",")(0)
        val miles = rows.split(",")(5).toInt
        (vehicle_id, miles)
    })
    
    basics.join(sales)
    .map(rows => rows._2._1 + "," + rows._2._2)
  }
}
