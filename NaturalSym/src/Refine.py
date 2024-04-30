from Caller import *
from Path import *
from utils import *
import sys, os, config, copy
from functools import reduce
import numpy as np

class Refine:
    def __init__(self, _outdir) -> None:
        self.outdir = _outdir
        pass
    
    def store_solution(self, path, caller):
        tables = caller.get_tables(path.rows)
        for input_id, rows in tables.items():
            filepath = os.path.join(self.outdir, "refined", input_id)
            #header = ",".join(path.shape[input_id])
            header = ",".join(["n/a" for i in range(len(path.schema[input_id]))])
            for row in rows: assert row.count(",") == header.count(",")
            caller._write(filepath, "\n".join([header] + rows))

    def main(self, path: Path):
        start_time = time.time()

        _limited = dict()
        valid_caller = None
        os.makedirs(os.path.join(self.outdir, "refined"), exist_ok=True)
        
        free_parts = [ivar for ivar in path.mutables if ivar in path._free_inputs]
        nonfree_parts = [ivar for ivar in path.mutables if ivar not in path._free_inputs]

        for ivar in free_parts:
            sampler = path.schema[get_inputid(ivar)][get_partid(ivar)]
            choices = sampler.samples(1)
            _limited[ivar] = choices

        for ivar in nonfree_parts:
            sampler = path.schema[get_inputid(ivar)][get_partid(ivar)]
            choices = sampler.samples(10)
            start_ = time.time()
            caller = path.invoke({**_limited, ivar: choices}, [])
            # print(caller.smt2path)
            # print("time cost", time.time() - start_)
            
            if caller.solution:
                _limited[ivar] = choices
                valid_caller = copy.deepcopy(caller)
            caller = None
        
        if valid_caller is None:
            valid_caller = path.invoke(_limited, [])

        end_time = time.time()
        self.time = end_time - start_time
        
        # I/O should not be taken into account
        self.store_solution(path, valid_caller)

        
        print(path.smt2path)
        print("Time cost: ", path.time)
        print("#Invocations: ", path.invocation)

        stat_filepath = os.path.join(self.outdir, "refined", "stats.txt")
        with open(stat_filepath, "w") as f:
            print(path.time, file=f)
            print(path.invocation, file=f)

def Run(benchmark, _seed, _GenDir):
    for _suffix in os.listdir(config.RunDir):
        np.random.seed(_seed)
        _smt2path = os.path.join(config.RunDir, _suffix)
        _outdir = os.path.join(_GenDir, benchmark, _suffix)
        if _smt2path.endswith(".smt2"):
            print(_smt2path)
            path = Path(_smt2path, config.schema_path(benchmark))
            Refine(_outdir).main(path)

if __name__ == "__main__":
    # assert len(sys.argv) >= 4, "args: benchmark, seed, GenDir"
    if len(sys.argv) == 2:
        Run(sys.argv[1], 43, config.GenDir)
    else:
        Run(sys.argv[1], int(sys.argv[2]), sys.argv[3]) 