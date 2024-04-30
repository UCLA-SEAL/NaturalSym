package symexScala

import scala.collection.mutable.ArrayBuffer
import java.util.HashSet
import ComparisonOp._

class UnexpectedInputType(message: String, cause: Throwable = null) extends RuntimeException(message, cause) {}

class JoinSymbolicResult(ss: SymbolicState, nonTerminatingPaths: Array[PathEffect], terminatingPaths: ArrayBuffer[TerminatingPath] = null, iVar: Array[SymVar], oVar: Array[SymVar])
    extends SymbolicResult(ss, nonTerminatingPaths, terminatingPaths, iVar, oVar) {


}

object JoinSymbolicResult {
  def apply(ss: SymbolicState, rddA: SymbolicResult, rddB: SymbolicResult): JoinSymbolicResult = {
    //Makes sure that A and B both have a more than one element as their symOutput
    require(rddA.symOutput.size > 1 && rddB.symOutput.size > 1, "join operators must be operated on tuples")
    
    // println(rddA.symOutput(0));
    // println(rddB.symOutput(0));
    require(rddA.keyLength == rddB.keyLength, "rddA should have the shape of key as rddB " + rddA.keyLength + " != " + rddB.keyLength);
    val numberOfKeys = if (rddA.keyLength != -1) rddA.keyLength else 1
    /*
    val keyA: SymVar = rddA.symOutput(0)
    val keyB: SymVar = rddB.symOutput(0)
    require(keyA != null && keyB != null);
    require(rddA.symOutput.size >= 2);
    require(rddB.symOutput.size >= 2);
    */

    val keysA: Array[SymVar] = rddA.symOutput.take(numberOfKeys)
    val keysB: Array[SymVar] = rddB.symOutput.take(numberOfKeys)
    require(rddA.symOutput.size > numberOfKeys);
    require(rddB.symOutput.size > numberOfKeys);
    for (key <- keysA) require(key != null);
    for (key <- keysB) require(key != null);


    //do join
    val product = new Array[PathEffect](rddA.numOfPaths * rddB.numOfPaths)
    // val joinedPaths = new Array[PathEffect](rddA.numOfPaths * rddB.numOfPaths)
    val terminatingPaths = new ArrayBuffer[TerminatingPath]()

    if (rddA.terminating != null) {
      terminatingPaths ++= rddA.terminating
    }
    if (rddB.terminating != null) {
      terminatingPaths ++= rddB.terminating
    }

    var i = 0
    for (pA <- rddA.paths) {
      for (pB <- rddB.paths) {
        product(i) = pB.conjunctPathEffect(pA)
        i += 1
      }
    }

    /*
    @thaddywu: deprecated, Sep 3, 2023
     Now, key could be not only soly a string, but several items
     ----------------
    for (i <- 0 until product.length) {
      //Assuming that the first element of symOutput array is the key
      //product(i) is the rest of the cluases and we need to replace A.key and B.key with the existential var in this rest!

      //Case 1:
      //  val c1 = new SymVar(keyA.actualType, ss.getFreshName)
      // val replacedC1: PathEffect = product(i).replace(keyA, c1).replace(keyB, c1)

      // val existA_B = new ExistentialConstraint(c1, replacedC1.pathConstraint.clauses)
      //existA_B.addCluase(ComparisonOp.isIn, keyA)
      //existA_B.addCluase(ComparisonOp.isIn, keyB)

      //  joinedPaths(i) = new PathEffect(existA_B, replacedC1.effects)

      //Case 2: Terminating
      //  val c2 = new SymVar(keyA.actualType, ss.getFreshName)
      //  val replacedC2: PathEffect = product(i)//.replace(keyA, c2).replace(keyB, c2)

      //  val existA_NotB = new ExistentialConstraint(c2, replacedC2.pathConstraint.clauses)
      // existA_NotB.addCluase(ComparisonOp.isIn, keyA)
      // existA_NotB.addCluase(ComparisonOp.isNotIn, keyB)

      val t1 = new Constraint(product(i).pathConstraint.clauses ++ Array(new Clause(keyA, ComparisonOp.Inequality, keyB)))
      val t2 = new Constraint(product(i).pathConstraint.clauses ++ Array(new Clause(keyA, ComparisonOp.Equality, keyB)))
      product(i).pathConstraint = t2

      terminatingPaths += new TerminatingPath(t1, product(i).effects)

      //Case 3: Terminating
      // val c3 = new SymVar(keyA.actualType, ss.getFreshName)
      // val replacedC3: PathEffect = product(i)//.replace(keyA, c3).replace(keyB, c3)

      //  val existNotA_B = new ExistentialConstraint(c3, replacedC3.pathConstraint.clauses)
      //   existNotA_B.addCluase(ComparisonOp.isNotIn, keyA)
      //  existNotA_B.addCluase(ComparisonOp.isIn, keyB)

      // terminatingPaths += new TerminatingPath(t1, product(i).effects)
      // @thaddywu: duplicated ? !!
    }*/


    // var result = ""
    // joinedPaths.foreach(result += _.toString+"\n")
    // println(result)

    
    for (i <- 0 until product.length) {
      val eq: Array[Clause]= new Array[Clause](numberOfKeys)

      for (j <- 0 until numberOfKeys) {
        val neq = new Constraint(product(i).pathConstraint.clauses ++ Array(new Clause(keysA(j), ComparisonOp.Inequality, keysB(j))))
        terminatingPaths += new TerminatingPath(neq, product(i).effects)
        eq(j) = new Clause(keysA(j), ComparisonOp.Equality, keysB(j))
      }

      product(i).pathConstraint = new Constraint(product(i).pathConstraint.clauses ++ eq)
    }

    // val input = rddA.symOutput ++ rddB.symOutput
    val input = rddA.symInput ++ rddB.symInput
    // @thaddywu: buggy

    // val output = Array(rddA.symOutput(0)) ++ rddA.symOutput.drop(1) ++ rddB.symOutput
    //  .drop(1)
    val output = keysA ++ rddA.symOutput.drop(numberOfKeys) ++ rddB.symOutput.drop(numberOfKeys)

    return new JoinSymbolicResult(ss, product, terminatingPaths, input, output)
    //return new JoinSymbolicResult(ss, product, terminatingPaths, input, output)

  }
}
