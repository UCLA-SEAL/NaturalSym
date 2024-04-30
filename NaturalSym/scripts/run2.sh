#!/bin/bash
# This script is to run NaturalSym five times for all benchmarks.
#  execute ./run2.sh

pushd `dirname ${BASH_SOURCE[0]}`
source env.sh
../BigTest/compile.sh || exit 1
../newbench/compile.sh || exit 1

source ../BigTest/init.sh

run2() {
    # bench, run_id, abs_stats_path, abs_gen_paths
    echo $1 "\n" $2 "\n" $3 "\n" $4 "\n"
    ../BigTest/bigtest.sh $1 $3/$1.txt || exit 1
    python3 ../src/Raw.py $1 $4 || exit 1
    python3 ../src/Refine.py $1 $id $4 || exit 1
}

rm ../newbench/m -r
for id in 1 2 3 4 5; do

mkdir -p ../newbench/m/geninputs${id}
mkdir -p ../newbench/m/bigtest_stats${id}
abs_geninputs_dir=`realpath ../newbench/m`/geninputs${id}
abs_bigtest_stats_dir=`realpath ../newbench/m`/bigtest_stats${id}

for b in "${!binpath[@]}"; do
 run2 $b $id ${abs_bigtest_stats_dir} ${abs_geninputs_dir} || exit 1
done

done


popd