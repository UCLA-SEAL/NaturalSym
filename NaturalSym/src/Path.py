from Caller import *
from utils import *
from Schema import *
import sys, os, config
from functools import reduce

# cvc5 statements
def _eq(x, y): return "(= {} {})".format(x, y)
def _neq(x, y): return "(not (= {} {}))".format(x, y)
def _and(x, y): return "(and {} {})".format(x, y)
def _or(x, y): return "(or {} {})".format(x, y)
def _not(x): return "(not {})".format(x)
def _assert(x): return "(assert {})".format(x)
def _assert_soft(x): return "(assert-soft {} :weight 1)".format(x)
def _andlist(al): return "true" if len(al) <= 0 else reduce(_and, al)
def _orlist(al): return "true" if len(al) <= 0 else reduce(_or, al)
def _stringfy(s): return s if s.startswith("\"") else "\"" + s + "\""
def _concat(x, y): return "(str.++ {} (str.++ \",\" {}))".format(x, y)
def _concatlist(al): return "\"\"" if len(al) <= 0 else reduce(_concat, al)
def _declstring(x): return "(declare-fun {} () String)".format(x)
def _contains(x, y): return "(str.contains {} {})".format(x, y)

class Path:
    def _print(self, content):
        if config.DEBUG:
            print("\u001B[33m[Path] " + str(content) + "\u001B[0m")

    def __init__(self, _smt2path, _schemapath=None) -> None:
        self.smt2path = _smt2path
        if _schemapath is not None:
            self.schema = parseSchema(_schemapath)
        else:
            self.schema = None

        self._decl = []
        self._asst = []
        self._split = []
        self._inquiry = ["(check-sat)", "(get-model)"]

        self._declared = []

        self.invocation = 0
        self.time = 0

        with open(self.smt2path, "r") as f:
            for line in f.readlines():
                line = line.strip("\n")
                if "(check-sat)" in line: continue
                if "(get-model)" in line: continue
                if line.startswith("(assert"):
                    if "; splitHandler" in line:
                        self._split.append(line)
                    else:
                        self._asst.append(line)
                else:
                    self._decl.append(line)
    
        def is_var_decl_stmt(l):
            # (declare-fun x5_P1 () String)
            return l.startswith("(declare-fun")
        def get_varname(l):
            return l.split(" ")[1]
        def get_vartype(l):
            return " ".join(l.split(" ")[3:]).strip(")")

        for l in self._decl:
            if is_var_decl_stmt(l):
                varname, vartype = get_varname(l), get_vartype(l)
                self._declared.append(varname)
        
        # add aux vars in split even if not used in any constraint
        self._split = [self._split_handler(line) for line in self._split]

        self.rows = [] # input1_P1
        self.inputs = [] # input1i, input1_d3
        for l in self._decl:
            if is_var_decl_stmt(l):
                varname, vartype = get_varname(l), get_vartype(l)
                if is_inputvar(varname): self.inputs.append(varname)
                if is_rowvar(varname): self.rows.append(varname)
        self.rows = sorted(self.rows)
        self.inputs = sorted(self.inputs)


        snippets = set()
        for stmt in self._asst:
            for piece in stmt.split(" "):
                snippets.add(piece)
        self._free_inputs = set([ivar for ivar in self.inputs if ivar not in snippets])

        # mutables
        self.mutables = []
        for ivar in self.inputs:
            if is_componentvar(ivar): self.mutables.append(ivar)
    
    def _split_handler(self, line):
        if self.schema is None: return line # no modifications
        ivar = line.split("splitHandler")[1].strip()
        if not is_rowvar(ivar): return line # not input components
        N = len(self.schema[get_inputid(ivar)])
        clist = [ivar + "_d" + str(i) for i in range(N)]

        no_comma_restrictions = []
        for var in clist:
            no_comma_restrictions.append(_not(_contains(var, "\",\"")))
            if var not in self._declared:
                self._declared.append(var)
                self._decl.append(_declstring(var))
        _split_assert = _eq(ivar, _concatlist(clist))
        #_split_assert = _and(_eq(ivar, _concatlist(clist)), _andlist(no_comma_restrictions))
        return _assert(_split_assert) + "; splitHandler {} {}".format(ivar, N)
    

    def _invoke(self, _add_stmt = None) -> Caller:
        self.invocation += 1
        if _add_stmt:
            stmts =  self._decl + self._split + _add_stmt + self._asst + self._inquiry
        else:
            stmts =  self._decl + self._split + self._asst + self._inquiry
        
        smt2_content = "\n".join(stmts) + "\n"
        caller = CVC5Caller(smt2_content)
        self.time += caller.time
        return caller

    def invoke(self, _limited, _negated) -> Caller:
        _add_stmt = []
        # _limited: dict(varname) -> ["a", "b", "c"]
        # _negated: [not [dict("input1")->"a"]]
                
        for varname, choices in _limited.items():
            _add_stmt.append(_assert(_orlist([_eq(varname, _stringfy(choice)) for choice in choices])))
        for negated in _negated:
            _tmp_list = [_eq(varname, _stringfy(solution)) for varname, solution in negated.items()]
            _add_stmt.append(_assert(_not(_andlist(_tmp_list))))
        
        return self._invoke(_add_stmt)

    def invoke_maxsat(self, _limited) -> Caller:
        _add_stmt = []
        # _limited: dict(varname) -> ["a", "b", "c"]
        # _negated: [not [dict("input1")->"a"]]
                
        for varname, choices in _limited.items():
            _add_stmt.append(_assert_soft(_orlist([_eq(varname, _stringfy(choice)) for choice in choices])))
        
        return self._invoke(_add_stmt)
    
if __name__ == "__main__":
    for _suffix in os.listdir(config.RunDir):
        _smt2path = os.path.join(config.RunDir, _suffix)
        if _smt2path.endswith(".smt2"):
            path = Path(_smt2path, config.schema_path(sys.argv[1]))
            caller = path.invoke(dict(), [])
            print("time cost", caller.time)
            print(caller.solution)