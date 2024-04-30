# Run actual big test
abspath=`dirname ${BASH_SOURCE[0]}`
export BigTest=`realpath $abspath`
echo BigTest: $BigTest

dependencies=$CLASSPATH
dependencies=$dependencies:$BigTest/jpf-core/build/*
dependencies=$dependencies:$BigTest/jpf-symbc/build/*
dependencies=$dependencies:$BigTest/SymExec/bin/*
dependencies=$dependencies:$BigTest/UDFExtractor/bin/*

declare -A binpath

NewBench=`realpath $BigTest/../newbench/bin/`
echo $NewBench
binpath["movie1"]=${NewBench}/movie1/movie1$
binpath["airport"]=${NewBench}/airport/airport$
binpath["usedcars"]=${NewBench}/usedcars/usedcars$
binpath["transit"]=${NewBench}/transit/transit$
binpath["credit"]=${NewBench}/credit/credit$
binpath["Q1"]=${NewBench}/Q1/Q1$
binpath["Q3"]=${NewBench}/Q3/Q3$
binpath["Q6"]=${NewBench}/Q6/Q6$
binpath["Q7"]=${NewBench}/Q7/Q7$
binpath["Q12"]=${NewBench}/Q12/Q12$
binpath["Q15"]=${NewBench}/Q15/Q15$
binpath["Q19"]=${NewBench}/Q19/Q19$
binpath["Q20"]=${NewBench}/Q20/Q20$