import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object grades {
  val conf = new SparkConf()
  conf.setMaster("local[*]")
  conf.setAppName("CommuteTime")
  val sc = new SparkContext(conf)
  sc.setLogLevel("ERROR")

  def main(args: Array[String]): Unit = {
    val maths = sc.parallelize(List("alice,90", "bob,10"))
    val physics = sc.parallelize(List("alice,10", "bob,20"))
    execute(maths, physics)
  }

  // execute is the method under test
  def execute(input1: RDD[String], input2: RDD[String]): Any = {
    // input1, input2 are students math/physics's grades
    //  - each input is a table, each rows contains two elements separated by coma
    //  - each rows contains a student's name and score
    // 
    // input1 : name,math
    // input2 : name,physics
    val maths = input1.map(row => (row.split(",")(0), row.split(",")(1).toInt))
    val physics = input2.map(row => (row.split(",")(0), row.split(",")(1).toInt))
    // parse the input row

    val joined_math_physics = maths.join(physics)
      //(name,math) join (name', physics) => (name, (math, physics)) if name=name'
      
    val sum_grades = joined_math_physics.map{
    row =>
      val name = row._1
      val math = row._2._1
      val physics = row._2._2
      (name, math + physics)
    }
    // each row is reshaped as (name, total_score)

    val filtered_rows = sum_grades.filter{
    row => 
      val name = row._1
      val total = row._2
      total < 60
    } // only retain students with total score < 60
    filtered_rows.foreach(println) // print each liene
  }
}
