pushd `dirname $BASH_SOURCE`

# pushd ../mutantGen
# sbt compile || exit 1
# ./run.sh || exit 1
# popd

echo Compiling scala programs, target jvm 1.5 # compatible with jad.
rm -rf bin
mkdir -p bin
scalac -target:jvm-1.5 -d bin -cp $CLASSPATH src/**/*.scala

popd