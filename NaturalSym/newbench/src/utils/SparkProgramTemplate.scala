
package utils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

trait SparkProgramTemplate { 
    def execute(input1: RDD[String]): RDD[_] = {null}
    def execute(input1: RDD[String], input2: RDD[String]): RDD[_] = {null}
    def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String]): RDD[_] = {null}
    def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String]): RDD[_] = {null}
    def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String]): RDD[_] = {null}
    def execute(input1: RDD[String], input2: RDD[String], input3: RDD[String], input4: RDD[String], input5: RDD[String], inputs6: RDD[String]): RDD[_] = {null}
    def execute(inputs: Array[RDD[String]]): RDD[_] = {
        if (inputs.length == 1) return execute(inputs(0))
        if (inputs.length == 2) return execute(inputs(0), inputs(1))
        if (inputs.length == 3) return execute(inputs(0), inputs(1), inputs(2))
        if (inputs.length == 4) return execute(inputs(0), inputs(1), inputs(2), inputs(3))
        if (inputs.length == 5) return execute(inputs(0), inputs(1), inputs(2), inputs(3), inputs(4))
        if (inputs.length == 6) return execute(inputs(0), inputs(1), inputs(2), inputs(3), inputs(4), inputs(5))
        require(false)
        null
    }
}