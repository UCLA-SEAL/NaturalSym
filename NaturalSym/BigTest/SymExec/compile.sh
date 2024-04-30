# symbc=/mnt/ssd/thaddywu/bigTest/jpf-symbc/build/jpf-symbc.jar
# core=/mnt/ssd/thaddywu/bigTest/jpf-core/build/jpf.jar
# udfextractor=/mnt/ssd/thaddywu/bigTest/BigTest/UDFExtractor/bin/udfExtractor.jar
symbc=../../jpf-symbc/build/jpf-symbc.jar # modified original jpf
core=../../jpf-core/build/jpf.jar # modified original jpf
udfextractor=../UDFExtractor/bin/udfExtractor.jar
scalac -cp $CLASSPATH:$symbc:$core:$udfextractor \
    -d bin src/**/*.scala && (pushd bin; jar -cvf SymExec.jar **/*.class; popd)