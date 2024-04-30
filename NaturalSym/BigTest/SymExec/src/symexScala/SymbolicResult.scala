package symexScala

import java.io.{BufferedWriter, File, FileWriter}
import java.util
import java.util.HashSet

import scala.collection.mutable.ArrayBuffer
import NumericUnderlyingType._
import ComparisonOp._
import ArithmeticOp._
import udfExtractor.SystemCommandExecutor
import sun.misc.ObjectInputFilter.Config
import udfExtractor.Configuration
import udfExtractor.Runner

class NotFoundPathCondition(message: String, cause: Throwable = null) extends RuntimeException("Not found Pa in C(A) for record " + message, cause) {}

/*
    paths = different paths each being satisfied by an equivalent class of tuples in dataset V
 */
class SymbolicResult(ss: SymbolicState, nonT: Array[PathEffect], t: ArrayBuffer[TerminatingPath], iVar: Array[SymVar], oVar: Array[SymVar], j: Boolean = false) {
  var LOOP_BOUND: Int  = 2
  val state: SymbolicState = ss
  val paths: Array[PathEffect] = nonT
  val terminating: ArrayBuffer[TerminatingPath] = t
  var symInput: Array[SymVar] = iVar
  var symOutput: Array[SymVar] = oVar
  var keyLength: Int = -1 // #elements in key
  
  def setKeyLength(_keyLength: Int): Unit = {keyLength = _keyLength;}

  override def toString: String = {
    var result = ""
    var id = 0
    result += "\u001B[32m";
    result += "input : " + symInput.mkString(",") + "\n";
    result += "output: " + symOutput.mkString(",") + "\n\n";
    result += "\u001B[0m";
    paths.foreach(p =>
      {id += 1; result += "\u001B[32m" + "Nonterminating #" + id + "\n" + p.toString + "\u001B[0m" + p.toShortZ3Query() + "\n"}
    )
    if (terminating != null)
      terminating.foreach(p =>
        {id += 1; result += "\u001B[32m" + "Terminating #" + id + "\n" + p.toString + "\u001B[0m" + p.toShortZ3Query() + "\n"}
      )
    result
  }
  def concludes(udf_name: String, invoke_smt: Boolean): Unit = {
    print("\u001B[32m");
    println("\n\n\n=============================");
    println("Function: " + udf_name);
    println("input : " + symInput.mkString(","));
    println("output: " + symOutput.mkString(","));

    var id = 0
    for (p <- paths) {
      id += 1;
      println("Nonterminating #" + id);
      println(p.toString);
      println(p.toShortZ3Query());
      print("\u001B[0m");
      if (invoke_smt) p.invokeSMT(true);
      print("\u001B[32m");
    }
    if (terminating != null)
    for (p <- terminating) {
      id += 1;
      println("Terminating #" + id);
      println(p.toString);
      println(p.toShortZ3Query());
      print("\u001B[0m");
      if (invoke_smt) p.invokeSMT(true);
      print("\u001B[32m");
    }
    print("\u001B[0m");
  }
  def filterInvalidPath(debug_mode:Boolean = false): SymbolicResult = {
    var new_path: ArrayBuffer[PathEffect] = new ArrayBuffer[PathEffect]
    var new_terminating: ArrayBuffer[TerminatingPath] = new ArrayBuffer[TerminatingPath]
    var existed_paths: HashSet[Int] = new HashSet[Int]
    for (p <- paths) {
      val hashcode = p.toZ3Query.hashCode
      if (!existed_paths.contains(hashcode)) {
        existed_paths.add(hashcode);
        val stdout = p.invokeSMT(debug_mode)
        if (!stdout.contains("unsat"))
          new_path += p;
      }
    }
    if (terminating != null)
    for (p <- terminating) {
      val hashcode = p.toZ3Query.hashCode
      if (!existed_paths.contains(hashcode)) {
        existed_paths.add(hashcode);
        val stdout = p.invokeSMT(debug_mode)
        if (!stdout.contains("unsat"))
          new_terminating += p;
      }
    }
    return new SymbolicResult(ss, new_path.toArray, new_terminating, symInput, symOutput);
  }
  def report(): Unit = {
    print("input: "); for (i <- symInput) print(i + ","); println();
    print("output: "); for (i <- symOutput) print(i + ","); println();
    var valid_path = 0;
    val Rundir = System.getenv("BigTest") + "/Rundir/";
    for (p <- paths) {
      val stdout = p.invokeSMT(true)
      if (!stdout.contains("unsat")) {
        val content = p.toZ3Query()
        valid_path += 1;
        p.writeTempSMTFile(Rundir + valid_path + ".smt2", content);
        stdout.split("\n").foreach(
          l => {
            if (l.contains("input") && !l.contains("_d"))
              println(l)
          }
        )
      }
    }
    if (terminating != null)
    for (p <- terminating) {
      val stdout = p.invokeSMT(true)
      if (!stdout.contains("unsat")) {
        val content = p.toZ3Query()
        valid_path += 1;
        p.writeTempSMTFile(Rundir + valid_path + ".smt2", content);
        stdout.split("\n").foreach(
          l => {
            if (l.contains("input"))
              println(l)
          }
        )
      }
    }

    println("#Valid paths: " + valid_path)
  }

  def numOfPaths: Int = { paths.size }

  def numOfTerminating: Int = {
    if (terminating != null) terminating.size
    else 0
  }

  /**
    *
    * Map
    * @param udfSymbolicResult symbolic execution output of a Udf
    * @result the combined SymbolicResult object
    *
    *
    * **/
  def map(udfSymbolicResult: SymbolicResult): SymbolicResult = {
    //returns Cartesian product of already existing paths *  set of paths from given udf

    val product =
      new Array[PathEffect](paths.size * udfSymbolicResult.numOfPaths)
    val product_terminating =
      // ArrayBuffer.fill((paths.size * udfSymbolicResult.numOfTerminating) + numOfTerminating)(new TerminatingPath(new Constraint()))
      ArrayBuffer.fill(numOfTerminating)(new TerminatingPath(new Constraint())) // disregard terminating path in map udf
    var i = 0
    var j = 0;
    var terminatingPaths = new ArrayBuffer[TerminatingPath]()
    if (this.terminating != null) {
      for (tp <- this.terminating) {
        product_terminating(j) = tp
        j += 1
      }

    }

    for (pa <- this.paths) {
      for (udfPath <- udfSymbolicResult.paths) {
        //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
        val link: Tuple2[Array[SymVar], Array[SymVar]] =
          if (this.symOutput != null)
            new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], this.symOutput.asInstanceOf[Array[SymVar]])
          else null

        product(i) = udfPath.conjunctPathEffect(pa, link)
        i += 1
      }
    }

    /*
    @thaddywu: may need consideration
    for (pa <- this.paths) {
      for (udfPath <- udfSymbolicResult.terminating) {
        //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
        val link: Tuple2[Array[SymVar], Array[SymVar]] =
          if (this.symOutput != null)
            new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], this.symOutput.asInstanceOf[Array[SymVar]])
          else null

        product_terminating(j) = udfPath.conjunctPathEffect(pa, link)
        j += 1
      }
    }
    */

    //val input =
    //  if (this.symInput.length == 0) udfSymbolicResult.symInput //@thaddywu: needs revision: why if branch?
    //  else this.symInput
    // new SymbolicResult(this.state, product, product_terminating, input, udfSymbolicResult.symOutput)
    new SymbolicResult(this.state, product, product_terminating, this.symInput, udfSymbolicResult.symOutput)
  }

  /**
    *
    * Reduce
    * @param udfSymbolicResult symbolic execution output of a Udf
    * @result the combined SymbolicResult object
    *
    *
    * **/
  def reduce(udfSymbolicResult: SymbolicResult): SymbolicResult = {
    //returns Cartesian product of already existing paths *  set of paths from given udf
    var arr_name = ss.getFreshName
    var arr_type = this.symOutput(0).actualType
    var type_name = arr_type match {
      case NonNumeric(t) =>
        CollectionNonNumeric(t)
      case Numeric(t) =>
        CollectionNumeric(t)
      case _ =>
        throw new UnsupportedOperationException("Not Supported Type " + arr_type.toString())
    }
    val symarray = new SymArray(type_name, arr_name)
    val arr_op_non = new SymArrayOp(type_name, ArrayOp.withName("select")) ///*** TODO: Only supporting Arrays of Integers
    
    
        // implementing the dynamic loop bound. 
    val symbolic_array: Array[Expr] = new Array[Expr](Runner.loop_bound())
 
    for (a <- 0 to Runner.loop_bound()-1){
      symbolic_array(a)  = new ArrayExpr(symarray, arr_op_non, Array(new ConcreteValue(Numeric(_Int), a.toString())))      
    }
    var i = 0
    val linked_paths = new Array[PathEffect](Math.pow(paths.size , Runner.loop_bound()).toInt)

    // Perform Cartesian product of the paths K times.
    var cartesian_paths  = crossArrays[PathEffect](Runner.loop_bound(), this.paths)
  
    for (paths_array <- cartesian_paths){
        linked_paths(i) = addOneToN_Mapping(this.symOutput(0), symbolic_array, paths_array)
        i = i + 1
        // TODO: Add constraints for similar key of both branches of the path
    }
  
    val product =
      new Array[PathEffect](linked_paths.size * udfSymbolicResult.numOfPaths)
    i = 0
    for (pa <- linked_paths) {
      for (udfPath <- udfSymbolicResult.paths) {
        //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
        val link: Tuple2[Array[SymVar], Array[SymVar]] =
          if (this.symOutput != null)
            new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], Array(symarray))
          else null

        product(i) = udfPath.conjunctPathEffect(pa, link) //@thaddywu: triky here, both set LOOP_BOUND as #inputs
        //    product(i) =
        //    product(i).addOneToN_Mapping(this.symOutput(1), Array(arr_0, arr_1))
        i += 1
      }
    }
    val input =
      if (this.symInput.length == 0) udfSymbolicResult.symInput
      else this.symInput
    new SymbolicResult(this.state, product, this.terminating, input, udfSymbolicResult.symOutput)
  }

  /****
   * Cartesian Product of the same array K times.
   * @param k times the array should be cartesian product
   * @param arr Array to be cartesian product
   * @result the cartesian product of the array with itself K times
   * 
   */
    def crossArrays[T](k : Int , arr:Array[T]): Array[ArrayBuffer[T]] = {
      if(k == 1)
      {
        var matrix = new Array[ArrayBuffer[T]](arr.length)
        for(a <- 0 to arr.length-1){
          matrix(a) = ArrayBuffer(arr(a))
        }
        return matrix;
      }
     else {
        for { x <- arr; y <- crossArrays(k-1, arr) } yield {y.append(x);y}
      }
    }
    
    
    /**
     * Re construct  paths and path constraints and effects after the loop unrolling 
     * @param link the input upstream Symbolic variable
     * @param arr the symbolic array of symbolic input
     * @param pa_array the paths from upstream operator
     * @result the re-named path constraints linked to the input 
     * **/
   def addOneToN_Mapping(link: SymVar, arr: Array[Expr], pa_array: ArrayBuffer[PathEffect], link_keys: Array[SymVar]=null): PathEffect = {
    val newEffects = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    if (link != null) {
      for (i <- 0 to arr.length - 1) {
        newEffects += new Tuple2(link.addSuffix("P" + (i + 1)), arr(i))
      }
    }
    var i = 1
    var clauses: Array[Clause] = Array()
    for(pa <- pa_array){
      for(e <- pa.effects){
        val newRHS: Expr = e._2.addSuffix("P" + i)
        val newLHS = e._1.addSuffix("P" + i)
        newEffects += new Tuple2(newLHS, newRHS)     
      }
      clauses  = clauses ++ pa.pathConstraint.addSuffix("P"+i).clauses
      if (link_keys != null && i != 1) {
        // @thaddywu: add constraint, groupByKey() {p1.key=p2.key=..}

        for (link_key: SymVar <- link_keys) {
          val link_clause: Clause = new Clause(link_key.addSuffix("P"+i), ComparisonOp.Equality, link_key.addSuffix("P1"))
          // println("link clause" + link_clause)
          clauses = clauses :+ link_clause
        }
      }
      i = i + 1 
    }
    new PathEffect(new Constraint(clauses), newEffects)
  }
    
    
  /**
    *
    * ReduceByKey
    * @param udfSymbolicResult symbolic execution output of a Udf
    * @result the combined SymbolicResult object
    *
    *
    * **/
    /* @thaddywu:
      We support (TupleN[String], Int) as the input now.
      Sep 3, 2023
    */
  def reduceByKey(udfSymbolicResult: SymbolicResult): SymbolicResult = {
    assert(this.symOutput.length >= 2, "ReduceByeKey is not Applicable, Effect of previous is not tuple")
    //returns Cartesian product of already existing paths *  set of paths from given udf

    val numberOfKeys = if (this.keyLength == -1) 1 else this.keyLength
    require(this.symOutput.length == numberOfKeys + 1, "ReduceByKey only accepts Tuple2(Tuple[String], Int)")
    val value_idx = numberOfKeys
    var arr_name = ss.getFreshName
    var arr_type = this.symOutput(value_idx).actualType
    println(arr_type)
    var type_name = arr_type match {
      case NonNumeric(t) =>
        CollectionNonNumeric(t)
      case Numeric(t) =>
        CollectionNumeric(t)
      case _ =>
        throw new UnsupportedOperationException("Not Supported Type " + arr_type.toString())
    }
    // implementing the dynamic loop bound. 
    val symbolic_array: Array[Expr] = new Array[Expr](Runner.loop_bound())
    
    val symarray = new SymArray(type_name, arr_name)
    val arr_op_non = new SymArrayOp(type_name, ArrayOp.withName("select")) ///*** TODO: Only supporting Arrays of Integers

    for (a <- 0 to Runner.loop_bound()-1){
      symbolic_array(a)  = new ArrayExpr(symarray, arr_op_non, Array(new ConcreteValue(Numeric(_Int), a.toString())))      
    }
    var i = 0
    val linked_paths = new Array[PathEffect](Math.pow(paths.size , Runner.loop_bound()).toInt)

    // Perform Cartesian product of the paths K times.
    var cartesian_paths  = crossArrays[PathEffect](Runner.loop_bound(), this.paths)
  
    for (paths_array <- cartesian_paths){
        linked_paths(i) = addOneToN_Mapping(this.symOutput(value_idx), symbolic_array, paths_array, this.symOutput.take(numberOfKeys)) 
          //@thaddywu: link keys
        i = i + 1
    }
    /*
    for (pa1 <- this.paths) {
      for (pa2 <- this.paths) {
        linked_paths(i) = pa1.addOneToN_Mapping(this.symOutput(1), symbolic_array, pa2)
        i = i + 1
        /**
        * TODO: Add constraints for similar key of both branches of the path
        * */
      }
    }
    * */

    
    
    val product =
      new Array[PathEffect](linked_paths.size * udfSymbolicResult.numOfPaths)
    i = 0
    for (pa <- linked_paths) {
      for (udfPath <- udfSymbolicResult.paths) {
        //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
        val link: Tuple2[Array[SymVar], Array[SymVar]] =
          if (this.symOutput != null)
            new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], Array(symarray))
          else null

        product(i) = udfPath.conjunctPathEffect(pa, link)
        //    product(i) =
        //    product(i).addOneToN_Mapping(this.symOutput(1), Array(arr_0, arr_1))
        i += 1
      }
    }
    val input =
      if (this.symInput.length == 0) udfSymbolicResult.symInput
      else this.symInput
    // val finalSymOutput = Array(this.symOutput(0)) ++ udfSymbolicResult.symOutput
    
    // val finalSymOutput: Array[SymVar] = this.symOutput.take(numberOfKeys) ++ udfSymbolicResult.symOutput
    // bug!! @thaddywu Sep 5, 2023
    //   As we expand the number of rows of this.symOutput
    //     the key after reduceByKey would become key_P1
    val new_keys = this.symOutput.take(numberOfKeys).map(key => key.addSuffix("P1"))
    val finalSymOutput = new_keys ++ udfSymbolicResult.symOutput

    new SymbolicResult(this.state, product, this.terminating, input, finalSymOutput)
  }

  /**
    *
    * FlatMap
    * @param udfSymbolicResult symbolic execution output of a Udf
    * @result the combined SymbolicResult object
    *
    *
    * **/
  def flatMap(udfSymbolicResult: SymbolicResult): SymbolicResult = {
  //  println("******************************** EDITS MADE *********")
    
    val product =
      new Array[PathEffect](paths.size * udfSymbolicResult.numOfPaths)
    val product_terminating =
      ArrayBuffer.fill((paths.size * udfSymbolicResult.numOfTerminating) + numOfTerminating)(new TerminatingPath(new Constraint()))
    var i = 0
    var j = 0;
    var terminatingPaths = new ArrayBuffer[TerminatingPath]()
    if (this.terminating != null) {
      for (tp <- this.terminating) {
        product_terminating(j) = tp
        j += 1
      }

    }

    for (pa <- this.paths) {
      for (udfPath <- udfSymbolicResult.paths) {
        //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
        val link: Tuple2[Array[SymVar], Array[SymVar]] =
          if (this.symOutput != null)
            new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], this.symOutput.asInstanceOf[Array[SymVar]])
          else null

        product(i) = udfPath.conjunctPathEffect(pa, link)
        i += 1
      }
    }

    for (pa <- this.paths) {
      for (udfPath <- udfSymbolicResult.terminating) {
        //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
        val link: Tuple2[Array[SymVar], Array[SymVar]] =
          if (this.symOutput != null)
            new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], this.symOutput.asInstanceOf[Array[SymVar]])
          else null

        product_terminating(j) = udfPath.conjunctPathEffect(pa, link)
        j += 1
      }
    }

    val input =
      if (this.symInput.length == 0) udfSymbolicResult.symInput // @thaddywu: can't be 0
      else this.symInput

    /*assert({
        udfSymbolicResult.symOutput(0).isInstanceOf[SymArray]
      || udfSymbolicResult.symOutput(0).isInstanceOf[StringOp]

    }, "Output of flatmap's udf is not an array")
     */

    val output_paths =
      new Array[PathEffect](paths.size * Runner.loop_bound)
    i = 0
    for (pa <- product) {
      // Fixed upper bound on the array -- Hard coded as K=2   -- Deprecated
      // Dynamic Upper Bound is implemented -- -8/18/2018
      for (a <- 1 to Runner.loop_bound){
        require(udfSymbolicResult.symOutput.size == 1, "accept only 1 output"); // we only allow one output
        output_paths(i) = pa.indexOutputArrayForFlatMap(udfSymbolicResult.symOutput(0).name, (a-1))
        i = i + 1
      }
    }

    new SymbolicResult(this.state, output_paths, product_terminating, input, udfSymbolicResult.symOutput)

  }

  /**
    *
    * Filter
    * @param udfSymbolicResult symbolic execution output of a Udf
    * @result the combined SymbolicResult object
    *
    *
    * **/
  def filter(udfSymbolicResult: SymbolicResult): SymbolicResult = {
    val product = new ArrayBuffer[PathEffect]()
    val terminatingPaths = new ArrayBuffer[TerminatingPath]()
    if (this.terminating != null) {
      terminatingPaths ++= this.terminating
    }

    for (udfPath: PathEffect <- udfSymbolicResult.paths) {
      //we need to check the effect to see whether it is a terminating or a non-terminating one
      // if it's terminating effect would be '0'
      if (udfPath.effects.last._2.toString == "0") { //terminating
      // @thaddywu: reviewed,
        val udfTerminating = new TerminatingPath(udfPath.pathConstraint)
        for (pa <- this.paths) {
          // print(pa.toString+" && "+udfTerminating.toString+" = ")
          //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
          val link: Tuple2[Array[SymVar], Array[SymVar]] =
            if (this.symOutput != null)
              new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], this.symOutput.asInstanceOf[Array[SymVar]])
            else null

          val conjuncted = udfTerminating.conjunctPathEffect(pa, link)
          terminatingPaths.append(conjuncted)
        }

      } else {
        val removedEffect = new PathEffect(udfPath.pathConstraint.deepCopy)
        for (pa <- this.paths) {
          //udf -> (x2, x3) / rdd -> (x0, x1) => link -> (x2 = x1)
          val link: Tuple2[Array[SymVar], Array[SymVar]] =
            if (this.symOutput != null)
              new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], this.symOutput.asInstanceOf[Array[SymVar]])
            else null
          product += removedEffect.conjunctPathEffect(pa, link)
        }
      }
    }

    // @thaddywu: add terminating paths from the udf
    /*
    if (udfSymbolicResult.terminating != null)
    for (udfPath: TerminatingPath <- udfSymbolicResult.terminating) {
        val udfTerminating = new TerminatingPath(udfPath.pathConstraint)
        for (pa <- this.paths) {
          val link: Tuple2[Array[SymVar], Array[SymVar]] =
            if (this.symOutput != null)
              new Tuple2(udfSymbolicResult.symInput.asInstanceOf[Array[SymVar]], this.symOutput.asInstanceOf[Array[SymVar]])
            else null

          val conjuncted = udfTerminating.conjunctPathEffect(pa, link)
          terminatingPaths.append(conjuncted)
        }
    }
    */

    //val input = 
      // if (this.symInput.length == 0) udfSymbolicResult.symInput
      // else this.symInput
    //udf symOutput is dis-regarded as it is either false or true
    //and since filter is side-effect free symInput is considered as output of whole
    //new SymbolicResult(this.state, product.toArray, terminatingPaths, input, udfSymbolicResult.symInput)

    new SymbolicResult(this.state, product.toArray, terminatingPaths, symInput, symOutput)
      // @thaddywu: as we modified the stored udf input as nullable, (when not used, leaved as null)
      //            we should take the output of previous operator as the output of .filter()
  }

  /**
    *
    * Join
    * @param secondRDD another SymbolicResult
    * @result the joined SymbolicResult object
    *
    *
    * **/
  def join(secondRDD: SymbolicResult): SymbolicResult = {
    JoinSymbolicResult.apply(this.state, this, secondRDD)

  }

  /**
    *
    * groupByKey
    *
    * **/
  // We need to spawn new branch to link the input of this operation to the output
  // E.g Input  : V -->  Output : [V1 ,V2] Such that V1 ==V, and V2 ==V
  def groupByKey(): SymbolicResult = {
    assert(this.symOutput.length >= 2, "GroupByKey is not Applicable, Effect of previous is not tuple")
    val product = new Array[PathEffect](paths.size * paths.size)
    var i = 0
    var arr_name = ss.getFreshName
    var arr_type = this.symOutput(1).actualType
    var type_name = arr_type match {
      case NonNumeric(t) =>
        CollectionNonNumeric(t)
      case Numeric(t) =>
        CollectionNumeric(t)
      case _ =>
        throw new UnsupportedOperationException("Not Supported Type " + arr_type.toString())
    }
    val symarray = new SymArray(type_name, arr_name)

    val arr_op_non = new SymArrayOp(type_name, ArrayOp.withName("select")) ///*** TODO: Only supporting Arrays of Integers
    val arr_0 =
      new ArrayExpr(symarray, arr_op_non, Array(new ConcreteValue(Numeric(_Int), "0")))
    val arr_1 =
      new ArrayExpr(symarray, arr_op_non, Array(new ConcreteValue(Numeric(_Int), "1")))
    for (pa1 <- this.paths) {
      for (pa2 <- this.paths) {
        //(x0, x1) -> (x2, [x3,x4] )  => link -> (x0 = x2) && (x1 = x3 , x4 = x1)
        //TODO: *****THIS IS WHERE WE NEED TO SPAWN A NEW LINK TO CONSTRUCT 1-N MAPPING BETWEEN INPUT AND OUTPUT
        product(i) = pa1.addOneToN_Mapping(this.symOutput(1), Array(arr_0, arr_1), pa2)
        //*******
        i += 1
      }
    }
    val input = this.symOutput
    val finalSymOutput = Array(this.symOutput(0)) ++ Array(symarray)
    new SymbolicResult(this.state, product, this.terminating, input, finalSymOutput)
  }

  // override def equals(other: Any): Boolean = {
  //     if(other != null && other.isInstanceOf[SymbolicResult]) {
  //         val castedOther = other.asInstanceOf[SymbolicResult]
  //         castedOther.numOfPaths == this.numOfPaths
  //     } else false
  // }

}

class EntrySymbolicResult(ss: SymbolicState, udf_name: String) 
  extends SymbolicResult(ss, Array(new PathEffect(new Constraint())), null, Array(ss.getInputVar(udf_name)), Array(ss.getInputVar(udf_name))) {
    
}
