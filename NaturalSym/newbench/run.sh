pushd `dirname $BASH_SOURCE`
#java -Xmx16G -cp bin:$CLASSPATH utils.TestSuite
mkdir bug_stats -p
testsDir=`realpath ~/NaturalFuzz/agg_geninputs/`
java -Xmx16G -cp bin:$CLASSPATH utils.BugFixCounter $testsDir/agg1/did bug_stats/1.out
java -Xmx16G -cp bin:$CLASSPATH utils.BugFixCounter $testsDir/agg2/did bug_stats/2.out
java -Xmx16G -cp bin:$CLASSPATH utils.BugFixCounter $testsDir/agg3/did bug_stats/3.out
java -Xmx16G -cp bin:$CLASSPATH utils.BugFixCounter $testsDir/agg4/did bug_stats/4.out
java -Xmx16G -cp bin:$CLASSPATH utils.BugFixCounter $testsDir/agg5/did bug_stats/5.out
popd 