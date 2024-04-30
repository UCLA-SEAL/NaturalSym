package utils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import java.io.File
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

object TestSuite { 
  val conf = new SparkConf()
  conf.setMaster("local[*]")
  conf.setAppName("CommuteTime")
  val sc = new SparkContext(conf)
  sc.setLogLevel("ERROR")
  var summary: String = ""
  def read(filepath: String, sc: SparkContext): RDD[String] = {
    if ((new File(filepath)).exists()) {
      sc.textFile(filepath).zipWithIndex()
        .filter(r => r._2 > 0).map(r => {
          /*
          if (r._1.endsWith(",")) r._1 + " "
          else r._1
          If the last column is empty, it will be discarded by .split(",").
          Adding a space is just a workaround.
          But on tpc-ds, the last column could be sometimes used as a key.
          */
          r._1 + ", "
        })
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

  val ResultDir = "./geninputs/" // may need to be fixed
  def testBench[T <: SparkProgramTemplate](benchmark: String, category: String, std: SparkProgramTemplate, faultys: Array[T], numberOfArgs: Int): (Int, Int, String) = {
    val file = new File(ResultDir + "/" + benchmark + "/")
    if (!file.exists()) return (0, 0, "")
    val paths = file.listFiles.map(path => path.getAbsolutePath)

    var detected = mutable.Set.empty[String]
    var summary = "";

    for (path <- paths) {
      val inputdir = path + "/" + category + "/"
      val inputs = Array.ofDim[RDD[String]](numberOfArgs)

      val pathId: String = path.split("/").lastOption.getOrElse("unknown").stripSuffix(".smt2")

      for (i <- 1 to numberOfArgs)
        inputs(i-1) = read(s"$inputdir/input$i", sc)

      println("\u001B[32m")
      for (i <- 1 to numberOfArgs)
        {println(s"input$i:"); inputs(i-1).foreach(println)}
      println("\u001B[0m")


      println(benchmark + ", " + category + ", " + pathId)
      val stdout = execute(std, inputs)


      summary = summary + "\n"
      summary = summary + benchmark + ", " + category + ", " + pathId + ": "
      summary = summary + "std: " + {if (stdout == "crashed") stdout else "ok"}

      println("stdout: " + stdout)

      for (tst <- faultys) {
        val name = tst.getClass.getSimpleName.toString.stripSuffix("$")

        println(name + ", " + category + ", " + pathId)
        val tstout = execute(tst, inputs)
        println(tstout)

        val status = {
          if (tstout.equals("crashed") && !stdout.equals("crashed")) "crashed"
          else if (tstout.equals(stdout)) "passed"
          else "failed"
        }
        if (status != "passed")
          summary = summary + ", " + name + ": " + status

        println("\u001B[33m")
        println(">" + benchmark + ", " + name + ", " + category + ", " + pathId + ", " + status)
        println("\u001B[0m")
        if (status != "passed" && !detected.contains(name))
          detected.add(name)
      }
    }
    (detected.size, faultys.length, detected.toString) //summary)
  }
  
  var seededFaults: String = "\\header{\\textsc{Seeded faults}}"
  var bigTest: String = "\\header{Detected by \\bigtest}"
  var naturalFuzz: String = "\\header{Detected by \\tool}"

  def testBench_wrapper[T <: SparkProgramTemplate](std: SparkProgramTemplate, faultys: Array[T], numberOfArgs: Int): Unit = {
    val benchmark = std.getClass.getSimpleName.toString.stripSuffix("$")
    val primitive_stats = testBench(benchmark, "primitive", std, faultys, numberOfArgs)
    val refined_stats = testBench(benchmark, "refined", std, faultys, numberOfArgs)

    summary = summary + s"$benchmark, ${primitive_stats._1}, ${refined_stats._1}, ${primitive_stats._2}\n"
    seededFaults += " & " + primitive_stats._2
    bigTest += " & " + primitive_stats._1
    naturalFuzz += " & " + refined_stats._1

    if (primitive_stats._1 != refined_stats._1) {
        println(s"primitive ($benchmark): ${primitive_stats._3}")
        println(s"refined ($benchmark): ${refined_stats._3}")
        summary += s"${primitive_stats._3} \n"
        summary += s"${refined_stats._3} \n"
    }
  }
  
  def main(args: Array[String]): Unit = {

    val movie1_faulty = Array(movie1WrongOperator, movie1WrongDelim, movie1WrongPredicate, movie1SwapKV, movie1WrongColumn)
    val usedcars_faulty = Array(usedcarsSwapKV) //Array(usedcarsWrongDelim, usedcarsWrongJoin, usedcarsWrongColumn, usedcarsWrongPredicate, usedcarsSwapKV)
    val airport_faulty = Array(airportWrongJoin, airportWrongPredicate, airportWrongOffset, airportSwapKV, airportWrongPredicate2)
    val transit_faulty = Array(transitWrongPredicate) // Array(transitWrongPredicate, transitSwapKV, transitWrongOffsets, transitWrongOperator, transitWrongColumn, transitWrongDelim)
      // transitWrongPredicate[*] path4 
    val credit_faulty = Array(creditWrongColumn, creditWrongDelim, creditWrongPredicate, creditWrongPredicate2, creditWrongPredicate3)
    // testBench_wrapper(usedcars, usedcars_faulty, 2)
    testBench_wrapper(transit, transit_faulty, 1)
/*
    testBench_wrapper(transit, transit_faulty, 1)
    testBench_wrapper(airport, airport_faulty, 2)
    testBench_wrapper(credit, credit_faulty, 1)
    testBench_wrapper(usedcars, usedcars_faulty, 2)
    testBench_wrapper(movie1, movie1_faulty, 1)
*/

    val Q1_faulty = Array(Q1_M0, Q1_M1, Q1SwapKV, Q1WrongColumn, Q1WrongDelim, Q1WrongPredicate, Q1WrongPredicate2)
      // Q1_M1 not detected, Q1SwapKV[*]
    val Q3_faulty = Array(Q3_M0, Q3_M1, Q3_M2, Q3_M3, Q3WrongPredicate, Q3WrongPredicate2, Q3SwapKV, Q3WrongOffset, Q3WrongDelim)
    val Q6_faulty = Array(Q6_M0, Q6_M1, Q6SwapKV, Q6WrongColumn, Q6WrongPredicate2, Q6WrongPredicate3)
    val Q7_faulty = Array(Q7_M0, Q7WrongKV2, Q7WrongKV, Q7WrongPredicate, Q7WrongPredicate3, Q7WrongPredicate2, Q7WrongColumn)
    val Q12_faulty = Array(Q12_M0, Q12WrongOffset, Q12SwapKV, Q12WrongColumn, Q12WrongPredicate)
    val Q15_faulty = Array(Q15_M0, Q15_M1, Q15WrongColumn, Q15WrongOffset, Q15SwapKV, Q15WrongPredicate, Q15WrongPredicate2)
    val Q19_faulty = Array(Q19_M0, Q19SwapKV, Q19SwapKV2, Q19WrongColumn, Q19WrongOffset, Q19WrongPredicate)
    //Q19_M0, 
    val Q20_faulty = Array(Q20_M0, Q20_M1, Q20WrongColumn, Q20SwapKV, Q20WrongJoin, Q20WrongPredicate, Q20WrongOffset)
/*
    testBench_wrapper(Q1, Q1_faulty, 4)
    testBench_wrapper(Q3, Q3_faulty, 3)
    testBench_wrapper(Q6, Q6_faulty, 5)
    testBench_wrapper(Q7, Q7_faulty, 5)
    testBench_wrapper(Q12, Q12_faulty, 3)
    testBench_wrapper(Q15, Q15_faulty, 4)
    testBench_wrapper(Q19, Q19_faulty, 6)
    testBench_wrapper(Q20, Q20_faulty, 3)
    */

    println("\u001B[31m")
    println("=" * 20)
    println(summary)
    println("=" * 20)
    println("\u001B[0m")

    println(seededFaults + " \\\\ \\hline")
    println(bigTest + " \\\\ \\hline")
    println(naturalFuzz + " \\\\ \\hline")
  }
}