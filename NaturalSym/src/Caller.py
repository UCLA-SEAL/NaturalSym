import os, sys, subprocess, time
from utils import *
import config
class Caller:
    # solution: (1) SAT, dict(): varname -> result
    #           (2) UNSAT, None
    # time: in sec
    def __init__(self, command, content) -> None:
        self.content = content
        smt2path = "/tmp/" + str(hash(content)) + ".smt2"
        self.smt2path = smt2path
        self._write(smt2path, content)
        self.stdout, self.time = self._execute(command + smt2path)
        
    def _print(self, content):
        if config.DEBUG:
            print("\u001B[32m[SMT] " + content + "\u001B[0m")

    def _write(self, filepath, content=None):
        if not content:
            content = self.content
        with open(filepath, "w") as f:
            f.write(content)
            f.flush()
            f.close()
    
    def _execute(self, command):
        self._print(command)
        start_time = time.time()
        subp = subprocess.run(command, shell=True, capture_output=True, text=True)
        end_time = time.time()
        return subp.stdout, end_time - start_time

    def get_tables(self, input_rows) -> dict:
        outputs = dict()
        for ivar in input_rows:
            table_id = get_inputid(ivar)
            if table_id not in outputs: outputs[table_id] = []
            outputs[table_id].append(self.solution[ivar])
        return outputs
    

class CVC5Caller(Caller):
    def __init__(self, content) -> None:
        super().__init__("cvc5 ", content)
        self.parseStdout()

    def parseStdout(self):
        if "unsat" in self.stdout:
            self.solution = None
        else:
            self.solution = dict()

            for stmt in self.stdout.split("\n"):
                stmt = stmt.strip()
                # (define-fun x0_d0 () String "A")
                # (define-fun x0 () String "A, , , , , ,")
                if stmt.startswith("(define-fun"):
                    varname = stmt.split(" ")[1]
                    if "() String \"" in stmt:
                        varsol = stmt.split("() String \"")[1]
                        assert varsol.endswith("\")")
                        varsol = varsol[:-2]
                    elif "() Int " in stmt:
                        varsol = stmt.split("() Int ")[1]
                        assert varsol.endswith(")")
                        varsol = varsol[:-1].strip()
                        if varsol.startswith("(") and varsol.endswith(")"):
                            assert varsol[1] == "-"
                            varsol = - int(varsol[3:-1])
                        else:
                            varsol = int(varsol)
                    self.solution[varname] = varsol
            # assert len(self.solution.keys()) > 0, self.stdout


class Z3Caller(Caller):
    def __init__(self, content) -> None:
        content = content.replace("(set-option :strings-exp true)", ";(set-option :strings-exp true)")
        content = content.replace("(set-logic QF_ASNIA)", ";(set-logic QF_ASNIA)")

        super().__init__("z3 ", content)
        self.parseStdout()
    def parseStdout(self):
        if "unsat" in self.stdout:
            self.solution = None
        else:
            self.solution = dict()
            assert False, "Z3 parseOut not implemented"