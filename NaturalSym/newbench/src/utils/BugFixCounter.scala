package utils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import java.io.File
import java.io._
import scala.Array
import utils.SparkProgramTemplate
import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

import movie1._
import usedcars._
import airport._
import transit._
import credit._
import Q1._
import Q3._
import Q6._
import Q7._
import Q12._
import Q15._
import Q19._
import Q20._

object BugFixCounter { 

  val paras = Map(
    "Q1" -> "$1/store_returns.csv $1/date_dim.csv $1/store.csv $1/customer.csv",
    "Q3" -> "$1/store_sales.csv $1/date_dim.csv $1/item.csv",
    "Q6" -> "$1/customer_address.csv $1/customer.csv $1/store_sales.csv $1/date_dim.csv $1/item.csv",
    "Q7" -> "$1/customer_demographics.csv $1/promotion.csv $1/store_sales.csv $1/date_dim.csv $1/item.csv",
    "Q12" -> "$1/web_sales.csv $1/date_dim.csv $1/item.csv",
    "Q15" -> "$1/catalog_sales.csv $1/customer.csv $1/customer_address.csv $1/date_dim.csv",
    "Q19" -> "$1/date_dim.csv $1/store_sales.csv $1/item.csv $1/customer.csv $1/customer_address.csv $1/store.csv",
    "Q20" -> "$1/catalog_sales.csv $1/date_dim.csv $1/item.csv",
    "airport" -> "$1/input1.csv $1/input2.csv",
    "credit" -> "$1/input1.csv",
    "movie1" -> "$1/input1.csv",
    "transit" -> "$1/input1.csv",
    "usedcars" -> "$1/input1.csv $1/input2.csv"
  )
  val faultys = Map(
    airport -> Array(airportWrongJoin, airportWrongPredicate, airportWrongOffset, airportSwapKV, airportWrongPredicate2),
    credit -> Array(creditWrongColumn, creditWrongDelim, creditWrongPredicate, creditWrongPredicate2, creditWrongPredicate3),
    usedcars -> Array(usedcarsWrongDelim, usedcarsWrongJoin, usedcarsWrongColumn, usedcarsWrongPredicate, usedcarsSwapKV),
    movie1 -> Array(movie1WrongOperator, movie1WrongDelim, movie1WrongPredicate, movie1SwapKV, movie1WrongColumn),
    transit -> Array(transitWrongPredicate, transitSwapKV, transitWrongOffsets, transitWrongOperator, transitWrongColumn, transitWrongDelim),
    // transitWrongPredicate[*] path4 
    Q1 -> Array(Q1_M0, Q1_M1, Q1SwapKV, Q1WrongColumn, Q1WrongDelim, Q1WrongPredicate, Q1WrongPredicate2),
    // Q1_M1 not detected, Q1SwapKV[*]
    Q3 -> Array(Q3_M0, Q3_M1, Q3_M2, Q3_M3, Q3WrongPredicate, Q3WrongPredicate2, Q3SwapKV, Q3WrongOffset, Q3WrongDelim),
    Q6 -> Array(Q6_M0, Q6_M1, Q6SwapKV, Q6WrongColumn, Q6WrongPredicate2, Q6WrongPredicate3),
    Q7 -> Array(Q7_M0, Q7WrongKV2, Q7WrongKV, Q7WrongPredicate, Q7WrongPredicate3, Q7WrongPredicate2, Q7WrongColumn),
    Q12 -> Array(Q12_M0, Q12WrongOffset, Q12SwapKV, Q12WrongColumn, Q12WrongPredicate),
    Q15 -> Array(Q15_M0, Q15_M1, Q15WrongColumn, Q15WrongOffset, Q15SwapKV, Q15WrongPredicate, Q15WrongPredicate2),
    Q19 -> Array(Q19_M0, Q19SwapKV, Q19SwapKV2, Q19WrongColumn, Q19WrongOffset, Q19WrongPredicate),
    //Q19_M0, 
    Q20 -> Array(Q20_M0, Q20_M1, Q20WrongColumn, Q20SwapKV, Q20WrongJoin, Q20WrongPredicate, Q20WrongOffset)
  )
  val baselines = Array("_hybrid", "_bigtest", "_naturalfuzz", "_pure", "_naturalsym", "_testminer", "_testminer_retrained", "_gpt")

  val conf = new SparkConf()
  conf.setMaster("local[*]")
  conf.setAppName("CommuteTime")
  val sc = new SparkContext(conf)
  sc.setLogLevel("ERROR")

  def read(filepath: String): RDD[String] = {
    // file should not contain header, an extra column in the end
    //  would indicate which path does the row come from
    println(filepath)
    if ((new File(filepath)).exists()) {
      sc.textFile(filepath).filter(_.length > 0)
    }
    else
      sc.emptyRDD[String]
  }
  def execute(prog: SparkProgramTemplate, inputs: Array[RDD[String]]): String = {
    try {
      val output: RDD[_] = prog.execute(inputs)

      return output.map(r => r + "]").collect().mkString("\n")
      // zipWithIndex().map(r => "Row " + r._2 + ":" + r._1)
    }
    catch {
      case e: Exception =>
        return "crashed";
    }
  }
  
  def main(args: Array[String]): Unit = {
    val tests_dir = args(0)
    var result: Map[String, String] = Map()
    for ((std, bugs) <- faultys)
    for (baseline <- baselines) {
        val bench_name = std.getClass.getSimpleName.toString.stripSuffix("$")
        val arguments = paras(bench_name).split(" ").map(arg => arg.substring(3))
        val rdds = arguments.map(arg => read(f"${tests_dir}/${baseline}/${bench_name}/${arg}"))
        var test_ids: Set[String] = Set()
        println(f"baseline ${baseline} benchmark ${bench_name}")
        rdds.foreach(rdd => rdd.collect().foreach(row => test_ids += row.split(",").last))

        var bugs_detected: Set[String] = Set()
        test_ids.foreach {
            test_id =>
                val rdds_id = rdds.map(rdd => rdd.filter(row => row.split(",").last == test_id))
                val stdout = execute(std, rdds_id)
                if (stdout != "crashed") 
                for (bug <- bugs) {
                    val bug_name = bug.getClass.getSimpleName.toString
                    if (!bugs_detected.contains(bug_name)) {
                        val aout = execute(bug, rdds_id)
                        if (aout != stdout) bugs_detected += bug_name
                    }
                }
        }
        val _rb = result.getOrElse(baseline, "") + "," + bench_name + ":" + bugs_detected.size.toString
        result += (baseline -> _rb)
    }
    
    result.foreach{case (bs, rs) => println(f"${bs} : ${rs}")}

    if (args.length > 1) {
      // write the result to args.length
        val writer = new PrintWriter(new File(args(1)))
        result.foreach {
          case (bs, rs) =>
            writer.write(f"${bs} ${rs}\n")
        }
        writer.close()
    }
  }
}