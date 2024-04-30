import CodeTransformUtils.{treeFromFile,writeTransformed}
import java.io.File
import scala.meta._
import FileUtils.writeToFile

object RunMutantGenerator {
  def main(args: Array[String]): Unit = {
    val inputFolder = "../newbench/src/"
    val testNames = List("Q1", "Q3", "Q6", "Q7", "Q12", "Q15", "Q19", "Q20")
    // val testNames = List("Q19")
    
    testNames.foreach {
      testName =>
        val outputFolder = s"$inputFolder/$testName"
        new File(outputFolder).mkdirs()
        val inputFile = s"$inputFolder/$testName/$testName.scala"
        val tree = treeFromFile(inputFile)


        val mutants = MutantGenerator.generateMutants(tree, testName)
        mutants
          .zipWithIndex
          .foreach {
          case ((transformed, mutantSuffix, mutantInfo), i) =>
            writeToFile(Seq(s"$mutantInfo"), s"$outputFolder/$mutantSuffix.info")
            writeTransformed(transformed.toString(), s"$outputFolder/${testName}_$mutantSuffix.scala")
        }
    }

  }
}