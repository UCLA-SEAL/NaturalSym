from Caller import *
from Path import *
from utils import *
from Distribution import *
import sys, os, config, copy
from functools import reduce
import numpy as np
import math


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

class RefineWithoutDistributions:
    def __init__(self, _outdir, _ablation) -> None:
        self.outdir = _outdir
        self.ablation = _ablation
        pass
    
    def store_solution(self, path, caller):
        tables = caller.get_tables(path.rows)
        for input_id, rows in tables.items():
            filepath = os.path.join(self.outdir, self.ablation, input_id)
            #header = ",".join(path.shape[input_id])
            header = ",".join(["n/a" for i in range(len(path.schema[input_id]))])
            for row in rows: assert row.count(",") == header.count(",")
            caller._write(filepath, "\n".join([header] + rows))

    def main(self, path: Path):
        start_time = time.time()

        os.makedirs(os.path.join(self.outdir, self.ablation), exist_ok=True)
        
        free_parts = [ivar for ivar in path.mutables if ivar in path._free_inputs]
        nonfree_parts = [ivar for ivar in path.mutables if ivar not in path._free_inputs]

        _add_stmt = []
        for ivar in path.mutables:
            sampler = path.schema[get_inputid(ivar)][get_partid(ivar)]
            L, R = sampler.getL(), sampler.getR()
            if L is not None:
                _add_stmt += ["(assert (<= {} (str.to_int {})))".format(math.ceil(L), ivar)]
            if R is not None:
                _add_stmt += ["(assert (<= (str.to_int {}) {}))".format(ivar, math.floor(R))]

            if isinstance(sampler, Discrete):
                #if self.ablation == "ablation": continue
                _add_stmt.append(_assert(_orlist([_eq(ivar, _stringfy(choice)) for choice in sampler.instances])))

        caller = path._invoke(_add_stmt)

        end_time = time.time()
        self.time = end_time - start_time
        
        # I/O should not be taken into account
        if caller.solution:
            self.store_solution(path, caller)

        
        print(path.smt2path)
        print("Time cost: ", path.time)
        print("#Invocations: ", path.invocation)

        stat_filepath = os.path.join(self.outdir, self.ablation, "stats.txt")
        with open(stat_filepath, "w") as f:
            print(path.time, file=f)
            print(path.invocation, file=f)

def Run(benchmark, ablation):
    for _suffix in os.listdir(config.RunDir):
        _smt2path = os.path.join(config.RunDir, _suffix)
        _outdir = os.path.join(config.GenDir, benchmark, _suffix)
        if _smt2path.endswith(".smt2"):
            print(_smt2path)
            path = Path(_smt2path, config.schema_path(benchmark))
            RefineWithoutDistributions(_outdir, ablation).main(path)

if __name__ == "__main__":
    np.random.seed(0)
    Run(sys.argv[1], "ablation")