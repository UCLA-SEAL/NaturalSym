package symexScala

import java.util.Vector
import java.util.Iterator
import gov.nasa.jpf.util.Pair
import gov.nasa.jpf.symbc.numeric.Expression
import gov.nasa.jpf.symbc.numeric.PathCondition
import gov.nasa.jpf.symbc.numeric.IntegerExpression
import gov.nasa.jpf.symbc.arrays.SelectExpression
import gov.nasa.jpf.symbc.numeric.RealExpression
import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression
import gov.nasa.jpf.symbc.numeric.IntegerConstant
import gov.nasa.jpf.symbc.numeric.SymbolicInteger
import gov.nasa.jpf.symbc.numeric.BinaryRealExpression
import gov.nasa.jpf.symbc.numeric.RealConstant
import gov.nasa.jpf.symbc.numeric.SymbolicReal
import gov.nasa.jpf.symbc.numeric.BinaryNonLinearIntegerExpression

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map

import NumericUnderlyingType._
import NonNumericUnderlyingType._
import gov.nasa.jpf.symbc.string.StringPathCondition
import gov.nasa.jpf.symbc.string.StringExpression
import gov.nasa.jpf.symbc.string.StringConstant
import gov.nasa.jpf.symbc.string.DerivedStringExpression
import gov.nasa.jpf.symbc.string.StringSymbolic
import gov.nasa.jpf.symbc.string.StringOperator
import gov.nasa.jpf.symbc.string.SymbolicLengthInteger
import gov.nasa.jpf.symbc.string.SymbolicCharAtInteger
import gov.nasa.jpf.symbc.mixednumstrg.SpecialIntegerExpression
import gov.nasa.jpf.symbc.PathEffectListener
import scala.collection.mutable.HashSet
import gov.nasa.jpf.symbc.arrays.ArrayExpression

class NotSupportedRightNow(message: String, cause: Throwable = null) extends RuntimeException("This is not supported right now: " + message, cause) {}

class PathEffectListenerImp extends PathEffectListener {

  var allPathEffects: Array[PathEffect] = null
  // @thaddywu: deprecated we use symState to record all the symvar info
  // val argsMap: Map[String, SymVar] = new Map[String, SymVar]() //from old names to instantiations with new names
  var symState: SymbolicState = null

  def convertRealExpression(lr: RealExpression): Expr = {
    lr match {
      case r: BinaryRealExpression => {
        val left: Expr = convertExpressionToExpr(r.getLeft()) //RealExpression -> Expr
        val right: Expr = convertExpressionToExpr(r.getRight()) //RealExpression -> Expr

        var opStr = r.getOp().toString().replaceAll("\\s", "")
        if (opStr != "+" && opStr != "-" && opStr != "*" && opStr != "/")
          throw new NotSupportedRightNow(opStr)
        val op = new SymOp(Numeric(_Double), ArithmeticOp.withName(opStr))
        new NonTerminal(left, op, right)
      }
      case c: RealConstant => new ConcreteValue(Numeric(_Double), c.toString())
      case s: SymbolicReal => symState.getSymVar(s.getName, "double")
      case _ => throw new NotSupportedRightNow(lr.getClass.getName)
    }
  }

  def convertIntegerExpression(li: IntegerExpression, isString: Boolean = false): Expr = {
    li match {
      case i: BinaryLinearIntegerExpression => {
        val left: Expr = convertExpressionToExpr(i.getLeft()) //IntegerExpression -> Expr
        val right: Expr = convertExpressionToExpr(i.getRight()) //IntegerExpression -> Expr

        var opStr = i.getOp().toString().replaceAll("\\s", "")
        if (opStr != "+" && opStr != "-" && opStr != "*" && opStr != "/")
          throw new NotSupportedRightNow(opStr)

        if (opStr == "/" && !right.isInstanceOf[ConcreteValue]) {
          val t = new Clause(right, ComparisonOp.Equality, new ConcreteValue(Numeric(_Int), "0"))
          terminating.add(new TerminatingPath(new Constraint(Array(t)))) // @thaddywu: = 0 ? nonterminating
        }
        val op = new SymOp(Numeric(_Int), ArithmeticOp.withName(opStr))
        new NonTerminal(left, op, right)
      }
      case c: IntegerConstant => {
        require(!isString)
        if (isString) {
          val ch: Char = c.toString.toInt.toChar
          new ConcreteValue(NonNumeric(_String), ch.toString())
        } else {
          new ConcreteValue(Numeric(_Int), c.toString())
        }
      }
      case s: SymbolicLengthInteger => // not supported: str.len
        val symstring = convertExpressionToExpr(s.parent)
        val opString = s.getName().replace("_1_", "")
        require(false, "SymbolicLengthInteger not supported")
        println("\u001B[32m" + "SymbolicLengthInteger: " + s.getName() + "\u001B[0m"); // @thaddywu
        val op = new SymStringOp(Numeric(_Int), StringOp.withName(opString))
        new StringExpr(symstring, op, Array())
      case s: SymbolicCharAtInteger => // not supported: chatAt
        val symstring = convertExpressionToExpr(s.se)
        val index = convertExpressionToExpr(s.index)
        val opString = s.getName().substring(0, s.getName().indexOf("("))
        require(false, "SymbolicLengthInteger not supported")
        println("\u001B[32m" + "SymbolicCharAtInteger: " + s.getName() + "\u001B[0m"); // @thaddywu
        val op =
          new SymStringOp(NonNumeric(_String), StringOp.withName(opString))
        new StringExpr(symstring, op, Array[Expr](index))
      case s: SymbolicInteger => symState.getSymVar(s.getName, "int")
      case sie: SpecialIntegerExpression => // valueof, only
        val symstring = convertExpressionToExpr(sie.opr)
        val opString = sie.getOp().name
        // println("\u001B[32m" + "SpecialIntegerExpression: " + opString + "\u001B[0m"); // @thaddywu
        val op = new SymStringOp(Numeric(_Int), StringOp.withName(opString))
        new StringExpr(symstring, op, Array[Expr]())

      case i: BinaryNonLinearIntegerExpression => {
        val left: Expr = convertExpressionToExpr(i.left) //IntegerExpression -> Expr
        val right: Expr = convertExpressionToExpr(i.right) //IntegerExpression -> Expr
        var opStr = i.op.toString().replaceAll("\\s", "")
        if (opStr != "+" && opStr != "-" && opStr != "*" && opStr != "/")
          throw new NotSupportedRightNow(opStr)
        val op = new SymOp(Numeric(_Int), ArithmeticOp.withName(opStr))
        if (opStr == "/") {
          val t = new Clause(right, ComparisonOp.Equality, new ConcreteValue(Numeric(_Int), "0"))
          terminating.add(new TerminatingPath(new Constraint(Array(t))))
        }
        new NonTerminal(left, op, right)
      }
      case _ => throw new NotSupportedRightNow(li.getClass.getName)
    }
  }

  def searchInputArrayName(name: String): String = {
    // @thaddywu: If the argument list contain arrays, but the udf contains another local array,
    //             searchInputArrayName may malfunction.
    val list: Vector[Pair[String, String]] = super.getArgsInfo()
    for (i <- 0 to list.size()) {
      if (list.get(i)._2.endsWith("[]")) {
        return list.get(i)._1
      }
    }
    require(false, "array name does not appear in the input")
    return name
  }

  def getArrayType(ae: ArrayExpression): String = {
    // getArrayType: ? [I@15b
    var elementtype = ae.getElemType()
    println("\u001B[32m" + "getArrayType: " + elementtype + " " + ae.getName + "\u001B[0m"); // @thaddywu
    if (elementtype.equals("?")) {
      var name = ae.getName
      name = name.replace("[", "");
      name.charAt(0) match {
        case 'I' =>
          return "Int"
        case _ =>
          throw new NotSupportedRightNow(name)
      }
    }
    else return elementtype
  }

  def convertSelectExpression(li: SelectExpression): Expr = {
    var ar = li.arrayExpression
    var name = searchInputArrayName(ar.getName) //@thaddywu: might be problematic?
    var i = li.indexExpression
    var indexexpr = convertIntegerExpression(i)
    //addInArgsMap(name + convertIntegerExpression(i) , name)
    // terminating path added   --- Check the ArrayINdexOut Of Bound exception
    val arr_type = getArrayType(ar)
    require(arr_type == "Int", "array other than int[] not supported");
    val symarray = symState.getSymVar(name, "int[]")
      // new SymArray(CollectionNumeric(NumericUnderlyingType.withName(arr_type)), name)
    val arr_op = new SymArrayOp(Numeric(_Int), ArrayOp.withName("length"))
    val arr_expr = new ArrayExpr(symarray, arr_op, Array())
    val t = new Clause(arr_expr, ComparisonOp.LessThanOrEq, indexexpr)
    //terminating.add(new TerminatingPath(new Constraint(Array(t))))
    //TODO: Uncomment this line to catch out of bound exception
    //@thaddywu: Terminating constraint ? but from the generated file, the constraint seems already added by SPF

    //non terminating
    val arr_op_non = new SymArrayOp(Numeric(NumericUnderlyingType.withName(arr_type)), ArrayOp.withName("select")) ///*** TODO: Only supporting Arrays of Integers
    new ArrayExpr(symarray, arr_op_non, Array(indexexpr))
    /**
    * The select array operation or array expression needs to be evaluated recursively.
    * Right now I am assuming that the name of the array is used all the time
    * **/
  }

  def convertStringExpression(se: StringExpression): Expr = {
    se match {
      case i: DerivedStringExpression => {
        val op = i.op
        println("\u001B[32mStringExpression: " + op.toString() + "\u001B[0m"); // @thaddywu
        if (i.oprlist == null) {
          var opStr = op.toString().replaceAll("\\s", "")
          val oper = new SymStringOp(NonNumeric(_String), StringOp.withName(opStr))
          if (oper.op == StringOp.Concat) {
            val right = convertExpressionToExpr(i.right)
            val left = convertExpressionToExpr(i.left)
            val oper = new SymStringOp(NonNumeric(_String), StringOp.withName(opStr))
            return new StringExpr(left, oper, Array(right))
          }
        }
        val stringsym = convertExpressionToExpr(i.oprlist(0))
        val len_par = i.oprlist.length
        var pars = new Array[Expr](len_par - 1)
        for (a <- 1 to len_par - 1) {
          pars(a - 1) = convertExpressionToExpr(i.oprlist(a))
        }
        pars = pars.reverse

        var opStr = op.toString().replaceAll("\\s", "")
        var oper: SymStringOp = null;
        //try {
          oper = new SymStringOp(NonNumeric(_String), StringOp.withName(opStr))
          if (oper.op == StringOp.Splitn && !split_symstr
                .contains(stringsym.toString())) {
            var index = pars(0)
            val t1 =
              new Clause(index, ComparisonOp.GreaterThan, new ConcreteValue(Numeric(_Int), "0"))
            val t2 =
              new Clause(stringsym, ComparisonOp.Equals, new ConcreteValue(NonNumeric(_String), ""))
            split_symstr.add(stringsym.toString())
            terminating.add(new TerminatingPath(new Constraint(Array(t1, t2))))
          }

        //} catch {
        //  case e: Exception =>
        //    throw new NotSupportedRightNow(opStr)
        //}
        return new StringExpr(stringsym, oper, pars) /// fix this
        /// Write implementation here
      }
      case c: StringConstant =>
        println("String constant: " + c.value())
        return new ConcreteValue(NonNumeric(_String), c.value())
      case s: StringSymbolic => {
        // println("\u001B[32m" + "StringSymbolic: " + s.getName() + "\u001B[0m"); // @thaddywu
        return symState.getSymVar(s.getName, "java.lang.String");
        //val intVar =
        //  argsMap.getOrElse(s.getName().replace("_SYMSTRING", ""), null)
        //if (intVar == null)
        //  return new SymVar(NonNumeric(_String), fixArrayName(s.getName()))
        //else intVar
      }
      case _ => throw new NotSupportedRightNow(se.getClass.getName)
    }
  }

  def convertExpressionToExpr(e: Expression): Expr = {
    e match {
      case li: IntegerExpression => convertIntegerExpression(li)
      //we are not supporting non-linear integer expr for now!
      case lr: RealExpression => convertRealExpression(lr)

      case se: SelectExpression => convertSelectExpression(se)

      case se: StringExpression => convertStringExpression(se)

      case _ =>
        throw new NotSupportedRightNow(e.getClass.getName)
    }
  }

  def convertConstraintToClause(cons: gov.nasa.jpf.symbc.numeric.Constraint): Clause = {
    val left: Expr = convertExpressionToExpr(cons.getLeft())
    val right: Expr = convertExpressionToExpr(cons.getRight())

    var compStr = cons.getComparator().toString().replaceAll("\\s", "")
    //if(compStr == "=") compStr = "=="
    val comp = ComparisonOp.withName(compStr)

    new Clause(left, comp, right)
  }

  def convertConstraintToClause(cons: gov.nasa.jpf.symbc.string.StringConstraint): Clause = {
    if (cons.getLeft != null) {
      val left: Expr = convertExpressionToExpr(cons.getLeft())
      val right: Expr = convertExpressionToExpr(cons.getRight())

      var compStr = cons.getComparator().toString().replaceAll("\\s", "")
      //if(compStr == "=") compStr = "=="
      val comp = ComparisonOp.withName(compStr)

      new Clause(left, comp, right)
    } else {
      val right: Expr = convertExpressionToExpr(cons.getRight())
      var compStr = cons.getComparator().toString().replaceAll("\\s", "")
      val comp = UniaryOp.withName(compStr)
      if (comp == UniaryOp.IsInteger) { // Teminating Paths for Intgerss
        val t = new UniaryClause(right, UniaryOp.NotInteger)
        terminating.add(new TerminatingPath(new Constraint(Array(t))))
      }
      new UniaryClause(right, comp)
    }
  }
  val terminating: HashSet[TerminatingPath] = new HashSet[TerminatingPath]();
  val split_symstr: HashSet[String] = new HashSet[String]();

  def convertPathCondition(pc: StringPathCondition): Constraint = {
    val clauses: ArrayBuffer[Clause] = new ArrayBuffer[Clause]()
    var current = pc.header //: gov.nasa.jpf.symbc.numeric.Constraint
    while (current != null) {
      clauses += convertConstraintToClause(current)
      val next = current.and
      current = next
    }
    /*  var clses  = List[Clause]()
        for((k,v) <- this.argsMap){
          clses = new Clause(new SymVar(Numeric(_Int),k),
              ComparisonOp.withName("=") ,
              v) ::clses
        }*/
    new Constraint(clauses.toArray) // ++ clses)
  }

  def convertPathCondition(pc: PathCondition, udfFileName: String): Constraint = {
    val clauses: ArrayBuffer[Clause] = new ArrayBuffer[Clause]()
    var current = pc.header //: gov.nasa.jpf.symbc.numeric.Constraint
    val s_constraints = convertPathCondition(pc.spc)
    while (current != null) {
      clauses += convertConstraintToClause(current)
      val next = current.and
      current = next
    }
    //var clses = List[Clause]()
    //for ((k, v) <- this.argsMap) {
    //  clses = new Clause(new SymVar(v.actualType, k), //+"_"+udfFileName),
    //                     ComparisonOp.withName("="),
    //                     v) :: clses
    //}
    // @thaddywu: fixed, this extra constraint is incorrect
    //  for example, when the udf input is x, and the new var is named x0
    //  we should substitue every x with x0, instead of simply adding one
    //  constraint x=x0, because in other udfs, the same var name x may exist,
    // however, they do not refer to the same var.

    // new Constraint(clauses.toArray ++ s_constraints.clauses ++ clses)
    new Constraint(clauses.toArray ++ s_constraints.clauses)
  }
  def convertAll(_symState: SymbolicState, udfFileName: String): SymbolicResult = {
    symState = _symState;
    symState.clearEnv();
    val pathVector: Vector[Pair[PathCondition, java.util.List[Expression]]] =
      super.getListOfPairs()
    val argsInfo: Vector[Pair[String, String]] = super.getArgsInfo()

    println("\u001B[32m" + "PathEffectListenerImp: " + udfFileName + "\u001B[0m");
    println("\u001B[32m" + "Java path: " + "./BigTest/Rundir/" + udfFileName + ".java" + "\u001B[0m");
    println("\u001B[32m" + "#paths:" + pathVector.size + " #args:" + argsInfo.size + "\u001B[0m");
    // @thaddywu: print udf symex result
    for (i <- 0 until pathVector.size)
      println("\u001B[32m" + "path #" + i + ": " + pathVector.get(i)._1 + " => " + pathVector.get(i)._2 + "\u001B[0m");
    for (i <- 0 until argsInfo.size)
      println("\u001B[32m" + "args #" + i + ": " + argsInfo.get(i)._1 + " => " + argsInfo.get(i)._2 + "\u001B[0m");

    require(argsInfo.size() > 0, "no argument is captured");
    // require(argsInfo.size() >= 1 && argsInfo.size() <= 3, "accept atmost 3 arguments for each udf");
    // June, 2023
    // Currently, we only support: Int, String, Tuple(,), Tuple(,Tuple(,))
    // At this point, they're already rewritten, as flatterned variables

    // Aug 25, 2023
    // Now, we support
    //  T: P | Tuple2(P, P) | Tuple2(String, T) | Tuple3[String] | Tuple4[String] | Tuple5[String]
    //  P: Integer | String
    require(pathVector.size > 0, "should have atleast one path"); //at lest one path
    var inputvars = new Array[SymVar](argsInfo.size);
    var outputvars = new Array[SymVar](pathVector.get(0)._2.size());
    
    // @thaddywu: output type was infered from the expression
    var allPathEffects = new Array[PathEffect](pathVector.size)
    for (i <- 0 until pathVector.size) {
      val pv = pathVector.get(i)
      val pathCondition = convertPathCondition(pv._1, udfFileName)
      val effectBuffer = new ArrayBuffer[Tuple2[SymVar, Expr]]()
      for (j <- 0 until pv._2.size) {
        val effect_j = convertExpressionToExpr(pv._2.get(j))
        if (i == 0)
          outputvars(j) = symState.createSymVar(udfFileName + "_" + j, effect_j.actualType)
        effectBuffer += new Tuple2(outputvars(j), effect_j)
      }
      allPathEffects(i) = new PathEffect(pathCondition, effectBuffer)
    }
    // inputvars must be retrieved after all path effects and conditions are converted
    //  If one input var is not used, the corresponding var will not be generated
    for (i <- 0 until argsInfo.size)
      inputvars(i) = symState.getSymVarIfExists(argsInfo.get(i)._1)

    //there is no terminating path in the scope of udf
    val ab = new ArrayBuffer[TerminatingPath]()
    terminating.map(s => ab.append(s))
    new SymbolicResult(symState, allPathEffects, ab, inputvars, outputvars)
  }

  /*
        assuming first input argument is our record (which also has the same type as return variable)
   */
  // @thaddywu: deprecated
  /*
  def convertAll(_symState: SymbolicState, udfFileName: String): SymbolicResult = {
    symState = _symState;
    val pathVector: Vector[Pair[PathCondition, java.util.List[Expression]]] =
      super.getListOfPairs()
    val argsInfo: Vector[Pair[String, String]] = super.getArgsInfo()

    // println("------>" + pathVector.size + " " + argsInfo.size)
    println("\u001B[32m" + "PathEffectListenerImp: " + udfFileName + "\u001B[0m");
    println("\u001B[32m" + "Java path: " + "/mnt/ssd/thaddywu/rinput/BigTest/Rundir/" + udfFileName + ".java" + "\u001B[0m");
    println("\u001B[32m" + "#paths:" + pathVector.size + " #args:" + argsInfo.size + "\u001B[0m");
    // @thaddywu: print udf symex result
    for (i <- 0 until pathVector.size)
      println("\u001B[32m" + "path #" + i + ": " + pathVector.get(i)._1 + " => " + pathVector.get(i)._2 + "\u001B[0m");
    for (i <- 0 until argsInfo.size)
      println("\u001B[32m" + "args #" + i + ": " + argsInfo.get(i)._1 + " => " + argsInfo.get(i)._2 + "\u001B[0m");

    require(argsInfo.size() >= 1 && argsInfo.size() <= 3);
    // Currently, we only support: Int, String, Tuple(,), Tuple(,Tuple(,))
    // At this point, they're already rewritten, as flatterned variables


    var (inputVar: Array[SymVar], outputVar: SymVar) =
      if (argsInfo.size == 1) {
        val freshVar: SymVar = symState.getFreshSymVar(argsInfo.get(0)._2)
        argsMap += (argsInfo.get(0)._1 -> freshVar)
        (Array(freshVar), symState.getFreshSymVar(argsInfo.get(0)._2))
      } else if (argsInfo.size == 2) {
        val freshVar: SymVar = symState.getFreshSymVar(argsInfo.get(0)._2)
        var f1 = new SymVar(SymbolicState.getVType(argsInfo.get(0)._2), freshVar.getName + "_1")
        var f2 = new SymVar(SymbolicState.getVType(argsInfo.get(1)._2), freshVar.getName + "_2")
        argsMap += (argsInfo.get(0)._1 -> f1)
        argsMap += (argsInfo.get(1)._1 -> f2
        (Array(f1, f2), symState.getFreshSymVar(argsInfo.get(0)._2))
      } else if (argsInfo.size == 3) {
        val freshVar: SymVar = symState.getFreshSymVar(argsInfo.get(0)._2)
        var f1 = new SymVar(SymbolicState.getVType(argsInfo.get(0)._2), freshVar.getName + "_1")
        var f2 = new SymVar(SymbolicState.getVType(argsInfo.get(1)._2), freshVar.getName + "_2_1")
        var f3 = new SymVar(SymbolicState.getVType(argsInfo.get(2)._2), freshVar.getName + "_2_2")
        argsMap += (argsInfo.get(0)._1 -> f1)
        argsMap += (argsInfo.get(1)._1 -> f2)
        argsMap += (argsInfo.get(2)._1 -> f3
        (Array(f1, f2, f3), symState.getFreshSymVar(argsInfo.get(0)._2))
      } else {
        for (i <- 0 until argsInfo.size) {
          println(argsInfo.get(i)._1 + " " + argsInfo.get(i)._2)
        }
        println("------------" + argsInfo.size + "-------------")
        throw new NotSupportedRightNow("more than 2 input arguments!")
      }
    
    for ((k, v) <- argsMap)
      println("\u001B[32m" + k + "->" + v + "\u001B[0m");
    
    // @thaddywu: output type was infered from the expression
    allPathEffects = new Array[PathEffect](pathVector.size())
    var outputV: Array[SymVar] = new Array[SymVar](pathVector.get(0)._2.size())
    for (i <- 0 until pathVector.size) {
      if (pathVector.get(i)._2.size() == 2) { // for tuple
        val effectFromSPF1: Expr = convertExpressionToExpr(pathVector.get(i)._2.get(0))
        val effectFromSPF2: Expr = convertExpressionToExpr(pathVector.get(i)._2.get(1))
        val effectBuffer = new ArrayBuffer[Tuple2[SymVar, Expr]]()
        outputV(0) = new SymVar(effectFromSPF1.actualType, outputVar.getName + "_1")
        outputV(1) = new SymVar(effectFromSPF2.actualType, outputVar.getName + "_2")
        effectBuffer += new Tuple2(outputV(0), effectFromSPF1)
        effectBuffer += new Tuple2(outputV(1), effectFromSPF2)
        allPathEffects(i) = new PathEffect(convertPathCondition(pathVector.get(i)._1, udfFileName), effectBuffer)
      } else {
        val effectFromSPF: Expr = convertExpressionToExpr(pathVector.get(i)._2.get(0))
        val effectBuffer = new ArrayBuffer[Tuple2[SymVar, Expr]]()
        outputV(0) = new SymVar(effectFromSPF.actualType, outputVar.getName)
        effectBuffer += new Tuple2(outputV(0), effectFromSPF)
        allPathEffects(i) = new PathEffect(convertPathCondition(pathVector.get(i)._1, udfFileName), effectBuffer)
      }

    }
    // for (pe <- allPathEffects)
    //  println("\u001B[33mNormal:\n" + pe.toShortZ3Query() + "\u001B[0m");
    // for (pe <- ab)
    //  println("\u001B[33mTerminating:" + pe.toShortZ3Query() + "\u001B[0m");
    //println(inputVar)
    //println(outputVar)
    //there is no terminating path in the scope of udf
    val ab = new ArrayBuffer[TerminatingPath]()
    terminating.map(s => ab.append(s))
    // println("getting terminating path constraints") @thaddywu commented
    new SymbolicResult(symState, allPathEffects, ab, inputVar, outputV)
  }
  */

    // @thaddywu: deprecated
  //def fixArrayName(str: String): String = {
  //  println("\u001B[32m" + "fixArrayName: " + str + "\u001B[0m"); // @thaddywu
  //  if (str.endsWith("_SYMSTRING")) {
  //    val name = str.replace("_SYMSTRING", "")
  //    val mod_name = name.replaceAll("_[0-9]+", "")
  //    if (this.argsMap.contains(mod_name))
  //      return mod_name
  //    else {
  //      name
  //    }
  //  }

  //  if (str.contains("SYMREF")) {
  //    val arr = str.split("_")
  //    val varname = arr(0)
  //    val dots_sep = str.split("\\.").drop(1)
  //    if (dots_sep.length == 0) {
  //      return varname;
  //    }
  //    return varname + dots_sep.reduce(_ + _)
  //  }

  //  if (str.endsWith("_SYMINT")) {
  //    val name = str.replace("_SYMINT", "")
  //    val mod_name = name.replaceAll("_[0-9]+", "")
  //    if (this.argsMap.contains(mod_name))
  //      return mod_name
  //    else {
  //      name
  //    }
  //  }

  // @ gulzar commented this
//      if(str.startsWith("[") &&  str.endsWith("]")){
//        val s = str.split("\\[")
//        val name  = searchInputArrayName(s(1))
//        val idx = s(2).replaceAll("\\]", "")
//        return name+idx
//      }
// @ end gulzar
  //  return str;
  //}
  /* @ gulzar commented this
  def addInArgsMap(str: String, arrname: String) = {
    var link = argsMap(arrname)
    argsMap.put(str, link)
  }
  */

  /* @commented by gulzar
    def compute(JPFDagNode j) : SymbolicResult = {
      val arr = new Array[SymbloicResult](j.parents)
      var i =o
      for(a <- j.parents){
        arr(i) = a.compute();
      }

      j.name match {
							case "map" =>
						  	   return arr(0).map(j.udfResult)
							case "filter" =>
						     return arr(0).fitler(j.udfResult)
							case "reduce" =>
					       return arr(0).reduce(j.udfResult)
							case "join" =>
							   return arr(0).join(arr(1))
							case _ =>
								throw new RuntimeException("This data flow operation is yet not supported!");
							}
    }
   * */
}
