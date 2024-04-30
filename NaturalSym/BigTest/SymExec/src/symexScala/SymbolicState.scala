package symexScala

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map

import NumericUnderlyingType._
import NonNumericUnderlyingType._

class SymbolicState() {
  val symbolicEnv: Map[String, SymVar] = Map[String, SymVar]()
  // val symbolicEnv: Map[String, SymbolicVarDef] = Map[String, SymbolicVarDef]()
  var index: Int = -1
  var loop_bound = 2

  def isDefined(x: SymVar): Boolean = {
    val found = symbolicEnv.getOrElse(x.getName, null)
    if (found != null && found.equals(x)) true
    else false
  }
  //returns null if no variable is defined under such a name!
  //def getSymVar(name: String): SymVar = {
  //  val found = symbolicEnv.getOrElse(name, null)
  //  if (found != null) found.variable
  //  else null
  //}

  // This may result in validate/check function not working
  // @thaddywu: We clear the env in order to avoid name conflit in different udfs.
  def clearEnv(): Unit = {
    symbolicEnv.clear()
  }

  def getVType(primitive: String): VType = {
    primitive match {
      case "int"              => Numeric(_Int)
      case "double"           => Numeric(_Double)
      case "int[]"            => CollectionNumeric(_Int)
      case "java.lang.String" => NonNumeric(_String)
      case _                  => NonNumeric(_Unit)
    }
  }

  def getFreshName: String = {
    index = index + 1
    "x" + index.toString
  }

  def pruneName(name: String): String = {
    if (name.endsWith("_SYMSTRING"))
      name.replaceAll("_[0-9]+_SYMSTRING", "")
    else if (name.endsWith("_SYMINT"))
      name.replaceAll("_[0-9]+_SYMINT", "")
    else
      name
  }
  def createSymVar(varName: String, vType: VType): SymVar = {
    val symVar = {
      if (vType == CollectionNumeric(_Int)) new SymArray(vType, this.getFreshName)
      else new SymVar(vType, this.getFreshName)
    }
    println("createSymVar: " + varName + " -> " + symVar.name)
    symbolicEnv += (varName -> symVar)
    return symVar
  }
  // name 'a_1_SYMINT' is first refined as `a`,
  //  if the symvar for `a` already exists, return the stored one
  //  otherwise, create a new symvar x1, and stored a->x1 in symbolicEnv
  def getSymVar(localName: String, primitive: String=null): SymVar = {
    val varName = pruneName(localName)
    if (symbolicEnv.contains(varName))
      return symbolicEnv.getOrElse(varName, null)
    else
      return createSymVar(varName, this.getVType(primitive))
  }
  def getSymVarIfExists(localName: String): SymVar = {
    val varName = pruneName(localName)
    return symbolicEnv.getOrElse(varName, null)
  }
  def getInputVar(input: String): SymVar = {
    val symVar = new SymVar(this.getVType("java.lang.String"), input)
    symbolicEnv += (input -> symVar)
    return symVar
  }


  //def getFreshSymVar(primitive: String): SymVar = {
  //  val vType = getVType(primitive)
  //  val varName = getFreshName
  //  val newVar = new SymVar(vType, varName)

  //  val newVarDef = new SymbolicVarDef(newVar)
  //  symbolicEnv += (varName -> newVarDef)

  //  newVar
  //}
}

//@thaddywu: deprecated
/*
class SymbolicVarDef(v: SymVar) {
  val variable: SymVar = v
  // var symbolicValue: Expr = v //initially it is same as symbolicVariable

  override def toString: String = {
    variable.toString
    // variable.toString + " -> " + symbolicValue.toString
  }

  // @thaddywu: deprecated
  //def updateEffect(effect: Expr) = {
  //  println("Variable " + v.getName + " updated from " + symbolicValue + " to " + effect)
  //  symbolicValue = effect
  //}
}
*/