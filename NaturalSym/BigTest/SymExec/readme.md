SymEx
VariableType.scala enumerations and class definitions of types
->Expr.scala enumerations of operators, symbolic terminals/nonterminals/stringExpr
    ->SymVar.scala classes of SymVar/SymArray
    ->Constraint.scala enumerations of constraint operators, classes of Clauses,  (conjunction of clauses) Constraint
	-> SymbolicState.scala the map: String->SymbolicVarDef(SymVar->Expr)
	-> PathEffect.scala Z3QueryState(init: String->Type, split:String->SplitHandler(str_arr: Array[String], del), replacements: String->String)
						definition of PathEffect
            -> SymbolicResult.scala Compositional symbolic info + SMT writing/calling + symex for spark operators.
	    -> JoinSymbolicResult

PathEffectListenerImp convert JPF constraint to custom constraint classes, build SymbolicResult

SymbolicState only supports primitive type int, int[], double, java.lang.String, Unit
