# Run NaturalSym on benchmarks
## 1. Configure and compile, using the comand below

> source ./scripts/env.sh

## 2. Run NaturalSym on the benchmark, movie1

> ./scripts/run.sh movie1

Benchmark List: `Q1 Q3 Q6 Q7 Q12 Q15 Q19 Q20 movie1 airport usedcars transit credit`

## 3. Observe the output
- program, e.g. `newbench/src/movie1/movie1.scala` 
- Buggy programs for mutation testing, e.g. `newbench/src/movie1/movie1WrongPredicate.scala`
- collected path, e.g. `newbench/geninputs/movie1/1.smt2/path.smt2` is the first symbolic path of movie1
- BigTest output, e.g. `newbench/geninputs/movie1/1.smt2/primitive/input1` is the input table 1 for the 1st path of movie1 by BigTest (big data analytics may take multiple tables as input)
```
,1910,,9,
,1910,,9,
```
- NaturalSym output, e.g. `newbench/geninputs/movie1/1.smt2/refined/input1` is the input table 1 for the 1st path of movie1 by NaturalSym
```
The Wolverine,1911,Fantasy,5,03:15
The Concubine,1942,Fantasy,5,03:00
```