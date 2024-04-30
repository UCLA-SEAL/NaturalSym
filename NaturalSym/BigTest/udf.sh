pushd `dirname ${BASH_SOURCE[0]}`
source init.sh
udf() {
    echo $@
    java -ea -cp "$dependencies" gov.nasa.jpf.JPF -udf $@
}
udf $@
popd