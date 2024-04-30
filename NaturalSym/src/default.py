from Caller import *
from Path import *
from utils import *
import sys, os, config, copy
from functools import reduce
import numpy as np


def store_solution(path, caller):
    ret = []
    tables = caller.get_tables(path.rows)
    for input_id, rows in tables.items():
        ret += [(input_id, "\n".join(rows))]
    return sorted(ret, key=lambda x: x[0])

def main(path: Path):
    start_time = time.time()

    _limited = dict()
    valid_caller = None
    
    free_parts = [ivar for ivar in path.mutables if ivar in path._free_inputs]
    nonfree_parts = [ivar for ivar in path.mutables if ivar not in path._free_inputs]

    for ivar in free_parts:
        sampler = path.schema[get_inputid(ivar)][get_partid(ivar)]
        if not sampler.isSpecified(): continue # not specified
        choices = sampler.samples(1)
        _limited[ivar] = choices

    for ivar in nonfree_parts:
        sampler = path.schema[get_inputid(ivar)][get_partid(ivar)]
        if not sampler.isSpecified():
            continue # not specified
        choices = sampler.samples(10)
        caller = path.invoke({**_limited, ivar: choices}, [])

        if caller.solution:
            _limited[ivar] = choices
            valid_caller = copy.deepcopy(caller)
        caller = None
    
    if valid_caller is None:
        valid_caller = path.invoke(_limited, [])
    
    return store_solution(path, valid_caller)

MaxPaths = 50
if __name__ == "__main__":
    RunDir = sys.argv[1]
    schema = parseSchema2(sys.argv[2])
    f = None
    if len(sys.argv) > 3 and sys.argv[3] is not None:
        output_file = sys.argv[3]
        f = open(output_file, "w")
    for pathid in range(1, MaxPaths):
        if os.path.exists(f"{RunDir}/{pathid}.smt2"):
            print(f"Generated tests for Path{pathid} in {RunDir}/{pathid}.smt2")
            path = Path(f"{RunDir}/{pathid}.smt2")
            path.schema = schema
            tests = main(path)
            print("\n".join([f"[{name}.csv]\n{test}" for (name, test) in tests]))
            print("\n"*2)
            if f is not None:
                print(f"Generated tests for Path{pathid} in {RunDir}/{pathid}.smt2\n", file=f)
                print("\n".join([f"[{name}.csv]\n{test}" for (name, test) in tests]), file=f)
                print("\n"*2, file=f)