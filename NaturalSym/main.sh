pushd `dirname ${BASH_SOURCE[0]}`

# run all benchmarks (BigTest + NaturalSym + Ablation study) once
./scripts/run1.sh all
# run all benchmarks (BigTest + NaturalSym) 5 times
./scripts/run2.sh
popd