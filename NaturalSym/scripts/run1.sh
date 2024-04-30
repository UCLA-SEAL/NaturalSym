#!/bin/bash

# This script is to run NaturalSym once for all benchmarks.
#  execute ./run1.sh all pr ./run1.sh <mnemonic of benchmark> such as ./run1/sh airport
#   entire benchmark list can be found in BigTest/init.sh

pushd `dirname ${BASH_SOURCE[0]}`
# (1) set up dependency path
# (2) compile the backend symbolic executor, BigTest
# (3) compile subject programs
source env.sh
../BigTest/compile.sh || exit 1
../newbench/compile.sh || exit 1

source ../BigTest/init.sh

BigTestStats=`realpath $BigTest/../newbench/`/bigtest_stats
mkdir -p $BigTestStats

run() {
    # $1 is the mnemonic of subject program, such as airport
    # run the backend symbolic executor to collect path constraints
    #  run Raw to get plain solution, Refine to use NaturalSym, RefineAblation for Ablation Study in section 4.5
    ../BigTest/bigtest.sh $1 $BigTestStats/$1.txt || exit 1
    python3 ../src/Raw.py $1 || exit 1
    python3 ../src/Refine.py $1 || exit 1
    python3 ../src/RefineAblation.py $1 || exit 1
}


if [ "$1" == "all" ]; then
    rm ../newbench/geninputs -r
    for b in "${!binpath[@]}"; do
     run $b || exit 1
    done
else
    run $1
fi
popd