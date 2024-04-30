javac -d bin src/**/*.java || exit 1
pushd bin
jar -cvf udfExtractor.jar **/*.class
popd