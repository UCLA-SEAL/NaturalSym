package udfExtractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Configuration extends Logging  {
    // static String JPF_HOME = "/mnt/ssd/thaddywu/bigTest/BigTest"; // Assuming that jpf/ contains jpf-core and jpf-symb
    // static String JD_CORE = "/mnt/ssd/thaddywu/.bin/jd-core/build/libs/jd-core-1.1.4.jar"; // jd-core
    // static String JUNIT_HOME = "/mnt/ssd/thaddywu/.bin/junit"; // Junit Home folder
    // static String JAVA_RUN_DIR = JPF_HOME + "/jpf-symbc/src/examples";
    // static String Z3_LIB = "/mnt/ssd/thaddywu/.bin/z3-src/build";
    // static String CFR_JAR = "/mnt/ssd/thaddywu/.bin/cfr/cfr-0.152.jar";
    // static String JAD_EXE = "/mnt/ssd/thaddywu/.bin/jad";
    // static String JAD_EXE = "jad";
    // static String Rundir =  "/mnt/ssd/thaddywu/bigTest/BigTest/Rundir/";
    static String BigTest = System.getenv("BigTest") ;
    static String Rundir = BigTest + "/Rundir/"; 
    // static String BenchBin = BigTest + "BenchmarksFault/bin/"; 
    // static String PYTHON_PATH = "/Users/malig/workspace/up_jpf/z3/build/python";

    static String arr[] = "map,flatMap,filter,reduceByKey,reduce,reduceByKey".split(",");
    static ArrayList<String> spark_ops = new ArrayList<>(Arrays.asList(arr));
    static HashMap<String, String> map_args = new HashMap<>();
    static JPFDAGNode program_dag = null;
    static int K_BOUND = 2;
    

    //// TODO: 9/14/17 Populate the input arguments to each of the udfs
    static String JPF_FILE_PLACEHOLDER(String target, String fun_name, String example_build, boolean isString , int numInputs) {
        String input = "sym";
        for(int i = 1 ; i < numInputs; i++)
            input += "#sym";
        
        String jpfContent = "";
        jpfContent += "target=" + target + "\n";
        jpfContent += "classpath=" + example_build + "\n";
        jpfContent += "symbolic.method=" + target + "." + fun_name + "(" + input +")\n";
        jpfContent += "symbolic.debug=false\n"; // -> false
        jpfContent += "symbolic.lazy=true\n";
        jpfContent += "symbolic.arrays=true\n";
        jpfContent += "symbolic.dp=no_solver\n";

        if (isString) {
            jpfContent += "symbolic.strings=true\n";
            jpfContent += "symbolic.string_dp_timeout_ms=3000\n";
            jpfContent += "search.depth_limit=20\n";
        }
        if (target.contains("reduce"))
            jpfContent += "search.multiple_errors=true\n";
        jpfContent += "listener = gov.nasa.jpf.symbc.SymbolicListener\n";
        jpfContent += "#listener = gov.nasa.jpf.symbc.sequences.SymbolicSequenceListener #for test-case generation\n";

        jpfContent += "report.console.start=\n"; // jpf,sut
        jpfContent += "report.console.transition=\n";
        jpfContent += "report.console.probe=\n"; // statistics
        jpfContent += "report.console.property_violation=\n"; // error,snapshot
        jpfContent += "report.console.constraint=\n"; // constraint,snapshot
        jpfContent += "report.console.finished=\n"; // result,statistics

        return jpfContent;
    }

    static boolean isSparkDataflowOperator(String op) {
        return spark_ops.contains(op);
    }

    static HashMap<String, String[]> dag_map  = new HashMap<String, String[]>(); 
    /* @thaddywu
    public static void readSPFInputArgs(String classname){
        try(BufferedReader br = new BufferedReader(new FileReader(classname ))) {
            for(String line; (line = br.readLine()) != null; ) {
               String arr[] = line.split(">");
                if(arr.length < 2) {
                    logerr("Invalid Configuration File");
                    return;
                }else{
                	if(!arr[0].startsWith("\\")) {
                		if(arr[0].startsWith("DAG")) {
                			//DAG > reduceByKey4-map5:map5-join:join-filter2,map3:filter2-map1
                			String edges[]  = arr[1].split(":");
                			for(int i = edges.length -1 ; i>=0 ; i--) {
                				//reduce-map3
                				String[] parents = edges[i].split("-")[1].split(",");
                				dag_map.put(edges[i].split("-")[0] , parents);
                			}
                			program_dag = JPFDAGNode.generateJPFDAGNode(dag_map , edges[0].split("-")[0]);
                		//Read Bound values from the configuration file. 
                		}else if(arr[0].startsWith("K_BOUND")){ 
                			K_BOUND = Integer.parseInt(arr[1]); 
                		}else {
                			Configuration.map_args.put(arr[0].trim(), arr[1].trim());
                            logdebug("Adding1 input arguments : " + arr[0].trim() + " --> " + arr[1].trim());
                		}
                	}
              }
            }
            // line is not visible here.
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    */
    static String getArgs(String op_name) {
        if (map_args.containsKey(op_name)) {
            return map_args.get(op_name);
        }
        return "";
    }
}