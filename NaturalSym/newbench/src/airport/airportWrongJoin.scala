package airport
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import utils.SparkProgramTemplate

object airportWrongJoin extends SparkProgramTemplate {
  def main(args: Array[String]): Unit = {
  }

  // report the gps location of all unclosed california airports with elevation < 100
  override def execute(input1: RDD[String], input2: RDD[String]): RDD[String] = {
    //ident, elevation_ft, iso_region, lat, long
    //ident, type
    val loc = input1.filter(airport => {
        val elevation_ft = airport.split(",")(1).toInt
        val iso_region = airport.split(",")(2)
        elevation_ft > 1000 && iso_region.substring(3,5).equals("CA")
    })
    .map(airport => {
        val ident = airport.split(",")(0)
        val gps_location = airport.split(",")(3) + "," + airport.split(",")(4)
        (ident, gps_location)
    })
    val not_closed = input2.filter(airport => {
        val status = airport.split(",")(1)
        !status.equals("closed")
    })
    .map(airport => (airport.split(",")(0), 1))

    loc.leftOuterJoin(not_closed) // Wrong join
    .map(joined => joined._2._1)
  }
}
