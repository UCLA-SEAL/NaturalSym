pushd `dirname $BASH_SOURCE`
rm ../newbench/src/*/*.info ../newbench/src/*/*_M*.scala
sbt "runMain RunMutantGenerator"
popd
# scala 2.11 not compatible