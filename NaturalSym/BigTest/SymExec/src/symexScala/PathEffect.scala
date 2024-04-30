package symexScala

import java.io.{BufferedWriter, File, FileWriter}
import scala.collection.mutable.ArrayBuffer
import java.util.HashSet
import scala.collection.mutable.HashMap
import java.util
import udfExtractor.SystemCommandExecutor

import NumericUnderlyingType._
import NonNumericUnderlyingType._

class UnionFind() {
  var parent = new HashMap[String, String]();
  var symvar = new HashMap[String, SymVar]();
  def clear(): Unit = {
    parent.clear();
  }
  def Find(x: String): String = {
    if (parent.contains(x)) {
      val px: String = parent.getOrElse(x, null)
      require(px != null)
      val a: String = Find(px)
      parent += (x -> a)
      a
    }
    else x
  }
  def Merge(x: SymVar, y: SymVar): Unit = {
    if (!symvar.contains(x.toString)) symvar += (x.toString -> x)
    if (!symvar.contains(y.toString)) symvar += (y.toString -> y)
    val a: String = Find(x.toString)
    val b: String = Find(y.toString)
    if (!a.equals(b)) {
      if (a.startsWith("input")) // always let input var be as the rep unit
        parent += (b -> a)
      else
        parent += (a -> b)
    }
  }
  def string2symvar(x: String): SymVar = {
    val y: SymVar = symvar.getOrElse(x, null);
    require(y != null);
    y
  }
}
class PathEffect(pc: Constraint, udfEffect: ArrayBuffer[Tuple2[SymVar, Expr]]) {
  var pathConstraint: Constraint = pc

  //TODO: change it back to effects after handling Spark DAG
  var effects: ArrayBuffer[Tuple2[SymVar, Expr]] = udfEffect

  def this(c: Constraint) {
    this(c, new ArrayBuffer[Tuple2[SymVar, Expr]]()) //no effects on variables
  }

  override def toString: String = {
    var eString: String = "| "
    for (ePair <- effects) {
      // sometimes, the input var may not be generated as it does not appear in the path constraint at all
      //  hence, the SymVar will be null in that case.
      require(ePair._1 != null, "path effect can't contain null vars");
      require(ePair._2 != null, "path effect can't contain null vars");
      eString += ePair._1.getName + "=" + ePair._2.toString + " | "
    }
    // if (effects.size > 0) eString = eString.substring(0, eString.length - 3)

    " path constraint: " + pathConstraint.toString + "\n" +
    " effect         : " + eString + "\n"
  }

  var uf: UnionFind = new UnionFind()
  def getEffectZ3Query(initial: Z3QueryState): String = {
    var eString: String = ""
    var rName: String = ""
    // val clauses: util.ArrayList[Clause] = new util.ArrayList[Clause]()
    val clauses: Array[Clause] = new Array[Clause](effects.size)
    var i = 0;
    for (e <- effects) {
      clauses(i) = new Clause(e._1, ComparisonOp.Equality, e._2)
      i = i + 1
    }
    val pathCond = new Constraint(clauses.toArray)
    return pathCond.toZ3Query(initial)
  }
  // @thaddywu
  def getEffectZ3QueryAndMergeSymvars(initial: Z3QueryState): String = {
    val clauses: ArrayBuffer[Clause] = new ArrayBuffer[Clause]()
    
    uf.clear();
    for (e <- effects)
      if (e._2.isInstanceOf[SymVar])
        uf.Merge(e._1, e._2.asInstanceOf[SymVar]);
    
    for (e <- effects)
      if (!e._2.isInstanceOf[SymVar]) {
      var cls = new Clause(e._1, ComparisonOp.Equality, e._2)
      
        // @thaddywu: added, vars in pc need renewal as well!
        //  June, 2023
      for ((k, v) <- uf.parent) {
        cls = cls.applyEffect(uf.string2symvar(k), uf.string2symvar(uf.Find(k)))
        // println("applyEffect " + k + " -> " + v)
        // Aug 25, 2023 @thaddywu
        //  Previous implementation of union-find set is incorrect
        //  as we should not use symvar as the key.
        //  Because, although .equals() is reloaded, but HashMap uses hashcode() first
      }
      clauses.append(cls); 
    }
    val pathCond = new Constraint(clauses.toArray)
    return pathCond.toZ3Query(initial)
  }

  // @thaddywu, some strings may be alias, we should use union-set algo to find those equality relations between strings
  //       before the split constraints are gened
  def generateSplitConstraints(state: Z3QueryState): String = {
    var s = ""
    for ((k, v) <- state.split) {
      var del = v.del
      val arr = v.str_arr
      // @thaddywu
      // error in \n in printed string
      if (del.contains("\n"))
        del = " \"\\n\" "
      //println("del:" + del)
      //println("arr:" + arr.mkString(","))

      for (i <- 0 until arr.length)
      if (arr(i) == null) {
        val new_name = k + "_d" + i.toString;
        arr(i) = new_name;
        state.init.add((new_name, NonNumeric(_String)));
      }

      val query = arr.reverse
        .map(s => if (s == null) " \"\" " else s)
        .reduce((a, b) => "(str.++ " + "(str.++ " + b + del + " )  " + a + ")")
      s = s + "\n" + s"""(assert (= ${k} ${query})) ; splitHandler ${k}"""
    }
    s
  }// @thaddywu, some strings may be alias, we should use union-set algo to find those equality relations between strings
  //       before the split constraints are gened
  def generateSubstrBoundaryConstraint(state: Z3QueryState): String = {
    var s = ""
    for ((k, v) <- state.substrmax) {
      s = s + "\n" + s"""(assert (>= (str.len ${k}) ${v})) """
    }
    s
  }

  def toShortZ3Query(): String = {

    val list: HashSet[(String, VType)] = new HashSet[(String, VType)]();

    val split = new HashMap[String, SplitHandler]();
    val replace = new HashMap[String, String]();
    val substrmax = new HashMap[String, String]();

    val state: Z3QueryState = Z3QueryState(list, split, replace, substrmax)

    var pc = pathConstraint.toZ3Query(state) + "\n" + getEffectZ3Query(state)
    s"""${generateSplitConstraints(state)}
           |$pc
           |
     """.stripMargin //,generateSplitConstraints(state))
  }
  def toZ3Query(): String = {

    val list: HashSet[(String, VType)] = new HashSet[(String, VType)]();

    val split = new HashMap[String, SplitHandler]();
    val replace = new HashMap[String, String]();
    val substrmax = new HashMap[String, String]();

    val state: Z3QueryState = Z3QueryState(list, split, replace, substrmax)

    var pc = getEffectZ3QueryAndMergeSymvars(state)
    var newPathConstraint: Constraint = pathConstraint
    
    for ((k, v) <- uf.parent)
    if (uf.symvar.contains(k))
      newPathConstraint = newPathConstraint.applyEffect(uf.string2symvar(k), uf.string2symvar(uf.Find(k)))
    pc += "\n" + newPathConstraint.toZ3Query(state)

    // @thaddywu: We apply effect before converting all constraints,
    //   this is for alias for split operator
    //    even though x0 = x2, but split(x0,0) and split(x2,0) may be
    //    converted into separate concatenation relationship, which
    //    makes the solving super slow.
    //   It seems that this feature was added by Gulzar, but later commented

    // var pc = pathConstraint.toZ3Query(state) + "\n" + getEffectZ3Query(state)

    pc = generateSplitConstraints(state) + "\n" + generateSubstrBoundaryConstraint(state) + "\n" + pc

    //fix the references
    // state.replacements += ("real.int.to.str" -> "int.to.str")
    // state.replacements += ("real.str.to.int" -> "str.to.int")
    for((k, v) <- state.replacements){
      // pc =  pc.replaceAll(v, k)
      pc = pc.replace(v, k)
    }
    // @thaddywu: optimization support for isinteger
    //  update: may introduce CVC4 choking

  // Warning: str.to.int => str.to_int
  // @thaddywu
    var decls = s"""
          |(set-logic QF_ASNIA)
          |(set-option :produce-models true)
          |(set-option :strings-exp true)
          |
          |
          |(define-fun isinteger ((x!1 String)) Bool (str.in.re x!1 (re.+ (re.range "0" "9")))  )
          |(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
          |(define-fun real.int.to.str ((i Int)) String (ite (< i 0) (str.++ "-" (int.to.str (- i))) (int.to.str i)))
          |(define-fun real.str.to.int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to.int (str.substr i 1 (- (str.len i) 1)))) (str.to.int i)))
          |
          |""".stripMargin
    // (define-fun isinteger ((x!1 String)) Bool (or (str.in.re x!1 (  re.++ (str.to.re "-") (re.+ (re.range "0" "9")))) (str.in.re x!1 (re.+ (re.range "0" "9"))) ) )
    // (define-fun isinteger ((x!1 String)) Bool (or (str.in.re x!1 (re.+ (re.range "0" "9")))  (str.in.re x!1 (  re.++ (str.to.re "-") (re.+ (re.range "0" "9")))) ) )
    val itr = state.init.iterator()
    while (itr.hasNext) {
      val i = itr.next()
      decls +=
        s"""(declare-fun ${i._1} () ${i._2.toZ3Query()})
                  |""".stripMargin
        //@thaddywu: still use declare-fun, for better compatability (cvc4/z3)
        //s"""(declare-const ${i._1} ${i._2.toZ3Query()})
        //          |""".stripMargin
    }
    var content: String = s"""$decls
           |$pc
           |(check-sat)
           |(get-model)
     """.stripMargin

    content = content.replace("str.to.int", "str.to_int");
    content = content.replace("int.to.str", "str.from_int");
    content = content.replace("str.in.re", "str.in_re");
    content = content.replace("str.to.re", "str.to_re");
    return content
  }
  //@thaddywu
  
  def writeTempSMTFile(filename: String, content: String): Unit = {
    try {
      val file: File = new File(filename)
      if (!file.exists) {
        file.createNewFile
      }
      val fw: FileWriter = new FileWriter(file)
      val bw = new BufferedWriter(fw)
      bw.write(content);
      bw.close();
    } catch {
      case ex: Exception =>
        ex.printStackTrace();
    }
  }

  def invokeSMT(debug_mode: Boolean=false): String =  {
    var content = toZ3Query();
    //var s = "cvc4-1.5 --strings-exp --lang smt2 < " + filename
    //content = content.replace("(set-option :strings-exp true)", "");
    //content = content.replace("(set-logic QF_ASNIA)", "");
    val filename = "/tmp/" + content.hashCode() + ".smt2";
    writeTempSMTFile(filename, content);

    var s = "cvc5 < " + filename
    // var s = "z3 " + filename

    if (debug_mode) println(s)
    try {
      val commands: util.List[String] = new util.ArrayList[String]
      commands.add("/bin/sh")
      commands.add("-c")
      commands.add(s)
      val commandExecutor: SystemCommandExecutor =
        new SystemCommandExecutor(commands, null)
      val result: Int = commandExecutor.executeCommand();
      val stdout: java.lang.StringBuilder =
        commandExecutor.getStandardOutputFromCommand
      val stderr: java.lang.StringBuilder =
        commandExecutor.getStandardErrorFromCommand
      
      val str_lines = stdout.toString.split("\n").filter(p => p.contains("error") || p.contains("sat"))
      
      if (debug_mode) {
        if(str_lines.size > 0) {
          print("\u001B[33m") // @thaddywu: hightlight output
          println(str_lines.reduce(_+"\n"+_))
          print("\u001B[0m") 
        }
        println("\n" + stderr.toString)
      }
      return stdout.toString;
    } catch {
      case e: Exception => {
        e.printStackTrace();
      }
      require(false, "CVC invocation failed.");
      return "error";
    }
  }
  /*
  def processOutput(str: String) {
    val arr = str.split("\n")
    val var_map = HashMap[String, String]()
//      arr.map(s => s.split(":")).filter(s => s.length>0).map{
//        s =>
//          var_map(s(0)) = s(1)
//      ""}
  }
  */

//    def generateFinalData(map: HashMap ): String = {
//      var s = ""
//      var buff = new ArrayBuffer[String]()
//      for( (k,v) <- state.split ){
//          val del = v.del
//          val arr = v.str_arr
//         // val query  = arr.reverse.map(s=> if(s==null) "\" \"" else s).reduce((a,b) => "(str.++ " + "(str.++ " + b +del+" )  " + a +")")
//             arr.filter(s => s!=null).map( a => buff.append(a))
//      }
//      return buff.filter(s=>s!=null).reduce(_+"\n"+_)
//    }
//
  override def equals(other: Any): Boolean = {
    if (other != null && other.isInstanceOf[PathEffect]) {
      val casted = other.asInstanceOf[PathEffect]
      casted.pathConstraint.equals(this.pathConstraint) && casted.effects
        .corresponds(this.effects)((a, b) => a._1.equals(b._1) && a._2.equals(b._2))
    } else false
  }

  /*
        conjuncts this(udf) PathEffect with already-existing rdd PathEffect
   */
  def conjunctPathEffect(rddPE: PathEffect, link: Tuple2[Array[SymVar], Array[SymVar]] = null): PathEffect = {
    val newEffects = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    rddPE.effects.copyToBuffer(newEffects)

    //adds the link between previous symOutput to the incoming symInput
    if (link != null) {
      var ar1 = link._1
      var ar2 = link._2
      if (ar1.size != ar2.size) {
        print("\u001B[33m");
        print("ar1: "); for (x <- ar1) print(x + ", "); println();
        print("ar2: "); for (x <- ar2) print(x + ", "); println();
        print("\u001B[0m");
      }
      require(ar1.size == ar2.size, "formal/actual parameters must match! (at least length)")
      // require(ar1.size <= ar2.size, "formal/actual parameters must match! (at least length)")
      // require(ar1.size == ar2.size, "formal/actual parameters must match! (at least length)")
      // @thaddywu: July 21, 2023
      //  Now, we allow length mismath, because input list length is inferred from UDF
      //  If y in Tuple(x,y) is not used, we can't infer how many primitive parts are inside y.
      for (i <- 0 to ar1.length - 1)
      if (ar1(i) != null && ar2(i) != null) {
        // @thaddywu: ar1-ar2 connects the preivous udf's output and this udf's input, but this udf's input may be empty.
        //  because, when a specific input var is not used in any path, we'll allocate a symvar for it.
        require(ar1(i).actualType == ar2(i).actualType, "linked vars must have the same type")
        newEffects += new Tuple2(ar1(i), ar2(i))
      }

    }
    newEffects.appendAll(this.effects)

    val newPathEffect = new PathEffect(rddPE.pathConstraint.deepCopy, newEffects)
    newPathEffect.pathConstraint.conjunctWith(this.pathConstraint)
    newPathEffect
  }

  // @thaddywu: reviewed, indexOutputArrayForFlatMap is only used in .flatMap()
  def indexOutputArrayForFlatMap(output_name: String, indx: Int): PathEffect = {
    var j = 0
    val newEffects = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    var tuned = false
    // var ret: Tuple2[SymVar, Expr] = null
    for (e <- effects) {
      if (e._1.name.equals(output_name)) {
        e._2 match {
          case StringExpr(_, op, _) =>
            if (op.op == StringOp.Split) {
              tuned = true
              val str_op = e._2.asInstanceOf[StringExpr]
              require(str_op.opr.size == 1, "split should have 1 element opr list") // @thaddywu: this function should only be called for dealing with .flatMap(.split())
              newEffects.append((e._1, new StringExpr(str_op.obj, new SymStringOp(op.actualType, StringOp.Splitn), Array(new ConcreteValue(Numeric(NumericUnderlyingType._Int), indx + "")) ++ str_op.opr)))
            } else {
              require(false, "Not a split expr")
              newEffects.append(e)
            }
          case _ =>
            require(false, "Not a string expr")
            newEffects.append(e)
        }
      } else {
        newEffects.append(e)
      }
    }
    require(tuned, output_name + " is not found in those path effects")
    new PathEffect(pathConstraint, newEffects)
  }

  // @thaddywu: reviewed, add _P1/_P2 to path effects pe1/pe2
  def addOneToN_Mapping(link: SymVar, arr: Array[Expr], pa2: PathEffect): PathEffect = {
    val newEffects = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    if (link != null) {
      for (i <- 0 to arr.length - 1) {
        newEffects += new Tuple2(link.addSuffix("P" + (i + 1)), arr(i))
      }
    }

    for (e <- this.effects) {
      val newRHS: Expr = e._2.addSuffix("P1")
      val newLHS = e._1.addSuffix("P1")
      newEffects += new Tuple2(newLHS, newRHS)
    }
    for (e <- pa2.effects) {
      val newRHS: Expr = e._2.addSuffix("P2")
      val newLHS = e._1.addSuffix("P2")
      newEffects += new Tuple2(newLHS, newRHS)
    }
    val cons1 = this.pathConstraint.addSuffix("P1")
    val cons2 = pa2.pathConstraint.addSuffix("P2")
    new PathEffect(new Constraint(cons1.clauses ++ cons2.clauses), newEffects)
  }

  def deepCopy: PathEffect = {
    val effectsCopy = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    if (this.effects.size != 0) {
      this.effects.copyToBuffer(effectsCopy)
    }
    new PathEffect(this.pathConstraint.deepCopy, effectsCopy)
  }

  //Shagha: Should return a new instance of PathEffect
  def replace(thisVar: SymVar, other: SymVar): PathEffect = {
    val effectsCopy = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    for (e <- this.effects) {
      val newRHS: Expr = e._2.replace(thisVar, other)
      if (e._1.equals(thisVar)) {
        effectsCopy += new Tuple2(thisVar, newRHS)
      } else effectsCopy += new Tuple2(e._1, newRHS)
    }
    val replacedPath = this.pathConstraint.replace(thisVar, other)
    new PathEffect(replacedPath, effectsCopy)
  }

  def addSuffix(sfx: String): PathEffect = {
    val effectsCopy = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    for (e <- this.effects) {
      val newRHS: Expr = e._2.addSuffix(sfx)
      val newLHS = e._1.addSuffix(sfx)
      effectsCopy += new Tuple2(newLHS, newRHS)
    }
    val replacedPath = this.pathConstraint.addSuffix(sfx)
    new PathEffect(replacedPath, effectsCopy)
  }
  /*
        returns a new instance of PathEffect
        by applying the given effect on to (this) instance's path condition and effects
        this instance should NOT be modified
   */
  /*
    def applyLastEffect(x: SymVar, lastEffect: Expr): PathEffect = {
        val newPathConstraint: Constraint = this.pathConstraint.applyEffect(x, lastEffect)
        val newEffect: Tuple2[SymVar, ArrayBuffer[Expr]] =
            if(this.effect != null) {
                val newEffectArray = this.effect._2.map(_.applyEffect(x, lastEffect))
                (this.effect._1, newEffectArray)
            } else null
        new PathEffect(newPathConstraint, newEffect)
    }
   */

  def checkValidity(ss: SymbolicState): Boolean = {
    var result = this.pathConstraint.checkValidity(ss)
    effects.foreach(effect => result &= effect._2.checkValidity(ss))

    result
  }

}

case class TerminatingPath(c: Constraint, u: ArrayBuffer[Tuple2[SymVar, Expr]]) extends PathEffect(c, u) {
  def this(c: Constraint) {
    this(c, new ArrayBuffer[Tuple2[SymVar, Expr]]()) //no effects on variables
  }
  /*
        conjuncts this(udf) PathEffect with already-existing rdd PathEffect
   */
  override def conjunctPathEffect(rddPE: PathEffect, link: Tuple2[Array[SymVar], Array[SymVar]] = null): TerminatingPath = {
    val newEffects = new ArrayBuffer[Tuple2[SymVar, Expr]]()
    rddPE.effects.copyToBuffer(newEffects)
    if (link != null) {
      var ar1 = link._1
      var ar2 = link._2
      require(ar1.size == ar2.size, "formal/actual parameters must match! (at least length)")
      for (i <- 0 to ar2.length - 1)
      if (ar1(i) != null && ar2(i) != null) {
        require(ar1(i).actualType == ar2(i).actualType, "linked vars must have the same type")
        newEffects += new Tuple2(ar1(i), ar2(i))
      }

    }
    newEffects.appendAll(this.effects)

    val newPathEffect =
      new TerminatingPath(this.pathConstraint.deepCopy, newEffects)
    newPathEffect.pathConstraint.conjunctWith(rddPE.pathConstraint)
    newPathEffect
  }

  override def checkValidity(ss: SymbolicState): Boolean = {
    this.pathConstraint.checkValidity(ss)
  }

}

case class Z3QueryState(init: HashSet[(String, VType)], split: HashMap[String, SplitHandler], replacements: HashMap[String, String], substrmax: HashMap[String, String]) {
  def addtoInit(a: (String, VType)) {
    val itr = init.iterator()
    while (itr.hasNext) {
      val de = itr.next()
      if (a._1.equals(de._1)) {
        require(a._2.equals(de._2), "type conflict in symvar decl")
        return
      }
    }
    init.add(a);
  }
}
case class SplitHandler(str_arr: Array[String], del: String)