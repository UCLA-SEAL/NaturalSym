# scala version: 2.11.12

#pushd ../jpf-core
#ant || exit 1
#popd
#pushd ../jpf-symbc
#ant || exit 1
#popd
pushd `dirname $BASH_SOURCE`

pushd ../jpf-core
ant || exit 1
popd
pushd ../jpf-symbc
ant || exit 1
popd

pushd UDFExtractor
./compile.sh || exit 1
popd
pushd SymExec
./compile.sh || exit 1
popd
pushd jpf-symbc
# ant clean || exit 1
ant || exit 1
popd
pushd jpf-core
# ant clean || exit 1
ant || exit 1
popd

popd