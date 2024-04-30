#!/bin/bash
src=`realpath $1` # source scala file under test (arg1)
cfg=`realpath $2` # config file (user annotation) (arg2)
prog=$(basename "$1" .scala)
hashv=`md5sum $src`
binpath="/tmp/${prog}\$"

pushd `dirname ${BASH_SOURCE[0]}` > /dev/null

source NaturalSym/scripts/env.sh  > /dev/null

BigTest=`realpath ./NaturalSym/BigTest`
NaturalSym=`realpath ./NaturalSym`

# 1. compile src (scala)
source $BigTest/init.sh > /dev/null
scalac -target:jvm-1.5 -d /tmp -cp $CLASSPATH $src
if [ $? -ne 0 ]; then
    echo -e "\033[31m(Step1) Failed to compile $1\033[0m"
    exit 1
fi

echo -e "\033[31m(Step1) Target scala program compiled => $binpath.class\033[0m"
# 2. compile BigTest
$BigTest/compile.sh > /dev/null || exit 1

# 3. run BigTest
pushd $BigTest
rm Rundir/* > /dev/null
java -ea -cp "$dependencies" gov.nasa.jpf.JPF -enableBT $binpath
if [ $? -ne 0 ]; then 
    echo -e "\033[31m(Step2) Failed to run BigTest, $1 is unsupported by SymEx engine.\033[0m"
    exit 1
fi

popd
echo -e "\033[31m(Step2) BigTest collected path conditions successfully => $BigTest/Rundir\033[0m"

popd > /dev/null

# 4. execute NaturalSym
python3 ${NaturalSym}/src/default.py ${BigTest}/Rundir $cfg $prog.tests

if [ $? -ne 0 ]; then 
    echo -e "\033[31m(Step3) Failed to run NaturalSym.\033[0m"
    exit 1
fi
echo -e "\033[31m(Step3) Completed! See ${prog}.tests \033[0m"

# example:
#  > ./naturalsym.sh grades.scala grades.config