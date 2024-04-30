pushd `dirname ${BASH_SOURCE[0]}`
#mylibpath=`realpath $1`
mylibpath=`realpath ../../_dependencies`
echo $mylibpath
export JAVA_HOME=$mylibpath/openlogic-openjdk-8u352-b08-linux-x64
export SCALA_HOME=$mylibpath/scala-2.11.12
export SPARK_HOME=$mylibpath/spark-2.4.0-bin-hadoop2.7 #2.1.0
export JUINT_HOME=$mylibpath/junit

export PATH=$JAVA_HOME/bin:$PATH # java path
export PATH=$SCALA_HOME/bin:$PATH # scala path
export PATH=$SPARK_HOME/bin:$PATH # spark path
export PATH=$PATH:$mylibpath # join my own path

export CLASSPATH=$CLASSPATH:$JUINT_HOME/ # junit
export CLASSPATH=$CLASSPATH:$mylibpath/jdt2/* #decompilers
export CLASSPATH=$CLASSPATH:$SPARK_HOME/jars/*

echo $CLASSPATH
popd