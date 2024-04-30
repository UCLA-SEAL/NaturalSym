from Caller import *
from Path import *
from utils import *
import sys, os, config
from functools import reduce

class Raw:
    def __init__(self, _outdir) -> None:
        self.outdir = _outdir
        pass
    
    def store_solution(self, path, caller):
        tables = caller.get_tables(path.rows)
        for input_id, rows in tables.items():
            filepath = os.path.join(self.outdir, "primitive", input_id)
            #header = ",".join(path.shape[input_id])
            header = ",".join(["n/a" for i in range(len(path.schema[input_id]))])
            for row in rows: assert row.count(",") == header.count(","), row + "\n" + \
                "header.len {} != row.len {} :table {} .smt2: {}".format(len(path.schema[input_id]), len(row.split(",")), input_id, path.smt2path)
            caller._write(filepath, "\n".join([header] + rows))

    def main(self, path: Path, wildcard=False):
        start_time = time.time()

        _limited = dict()
        if wildcard:
            free_parts = [ivar for ivar in path.mutables if ivar in path._free_inputs]
            for ivar in free_parts:
                _limited[ivar] = ["_"]

        valid_caller = path.invoke(_limited, [])

        end_time = time.time()
        self.time = end_time - start_time

        assert valid_caller.solution is not None, "initial smt is non-satisfiable"
        os.makedirs(os.path.join(self.outdir, "primitive"), exist_ok=True)
        filepath = os.path.join(self.outdir, "path.smt2")
        valid_caller._write(filepath)
        
        self.store_solution(path, valid_caller)

        stat_filepath = os.path.join(self.outdir, "primitive", "stats.txt")
        with open(stat_filepath, "w") as f:
            print(path.time, file=f)

def Run(benchmark, GenDir):
    for _suffix in os.listdir(config.RunDir):
        _smt2path = os.path.join(config.RunDir, _suffix)
        _outdir = os.path.join(GenDir, benchmark, _suffix)
        if _smt2path.endswith(".smt2"):
            path = Path(_smt2path, config.schema_path(benchmark))
            Raw(_outdir).main(path)

if __name__ == "__main__":
    # assert len(sys.argv) >= 3, "args: benchmark, GenDir " + sys.argv
    if len(sys.argv) == 2:
        Run(sys.argv[1], config.GenDir)
    else:
        Run(sys.argv[1], sys.argv[2]) 
    