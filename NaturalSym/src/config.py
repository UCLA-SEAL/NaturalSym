# debug mode, verbose level.
DEBUG = False

import os
RootDir = "/".join(os.path.abspath(__file__).split("/")[:-2])
RunDir = f"{RootDir}/BigTest/Rundir/"
GenDir = f"{RootDir}/newbench/geninputs/"
SchemaDir = f"{RootDir}/newbench/config/"

def schema_path(prog):
    return "{}/{}.config".format(SchemaDir, prog)