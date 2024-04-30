package udfExtractor;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.*;

public class SparkProgramVisitor extends ASTVisitor {

    HashMap<String, ArrayList<String>> call_graph = new HashMap<String, ArrayList<String>>();
    UDFDecompilerAndExtractor log = null;
    String jpf_dir = null;
    // ASTRewrite rewrite; @thaddywu: deprecated

    public SparkProgramVisitor(UDFDecompilerAndExtractor l, String output_jpf , ASTRewrite rw) {
        log = l;
        jpf_dir = output_jpf;
        // rewrite = rw;
    }

    // Set names = new HashSet();
    HashMap<String, String> code2udfid = new HashMap<>(); // @thaddywu: code snippet -> corresponding udf
    String methodDeclName = ""; //@thaddywu: wrapping method decl name
    // String invokedMethod = null; //@thaddywu: xxx.map(...) xxx.join(..) @deprecated
    Stack<String> udfStack = new Stack<>(); //@thaddywu: when udf is created, record the udf name
    boolean inUDF = false;
    // String parameters = null; // @thaddywu: deprecated

    /* @thaddywu: deprecated
    public boolean visit(ClassInstanceCreation cls) {
        // System.out.println("\u001B[32m");
        // System.out.println("ClassInstanceCreation");
        // System.out.println(cls);
        // System.out.println("============");
        // System.out.println("\u001B[0m");
        
        // .. .map( new Serializable() {})
        if (invokedMethod != null)
            if (Configuration.isSparkDataflowOperator(invokedMethod) && cls.getType().toString().equals("Serializable")) {
                startUDFClass();
                cls.getAnonymousClassDeclaration().accept(this);
                closeUDFClass();
                return false;
                // @thaddywu: Here, we could return false to avoid replicating traversal
            }
        return true;
    }
    */

    HashMap<String, String> typeMapping = new HashMap<>();

    // @ thaddywu: return udf name, or input (both corresponds to a RDD) 
    public String getCorrespondingUDForInput(ASTNode node) {
        if (node instanceof SimpleName) {
            // input1 (input) or joined (tmp var)
            String name = ((SimpleName) node).getIdentifier().toString();
            return name.startsWith("input") ? name : code2udfid.get(name);
        }
        if (node instanceof MethodInvocation) {
            // RDD$.MODULE$.rddToPairRDDFunctions(trips, ClassTag$.MODULE$.apply(java/lang/String), ClassTag$.MODULE$.Int(), scala.math.Ordering.String..MODULE$)
            MethodInvocation inv = (MethodInvocation) node;
            String methodName = inv.getName().getIdentifier().toString();
            if (methodName.equals("rddToPairRDDFunctions")) {
                ASTNode p0 = (ASTNode) inv.arguments().get(0); // p0 may not be a SimpleName instance
                //RDD$.MODULE$.rddToPairRDDFunctions(joined.map(..))
                return getCorrespondingUDForInput(p0);
            }

            if (methodName.equals("join") || Configuration.isSparkDataflowOperator(methodName)) {
                return code2udfid.get(node.toString());
            }
        }

        assert (false);
        return null;
    }

    public boolean visit(CastExpression ce) {
        // System.out.println("\u001B[36m");
        // System.out.println("CastExpression");
        // System.out.println(ce);
        // System.out.println("============");
        // System.out.println("\u001B[0m");
        //log.logdebug(ce.getType().toString());
        //log.logdebug(ce.getExpression().toString());
        typeMapping.put(ce.getExpression().toString(), ce.getType().toString());
        //SimpleName s = ce.getAST().newSimpleName(parameters+"_t2");

       //rewrite.replace(ce.getType() , ce.getAST().newSimpleName("") ,null);
        //ce.getExpression().accept(this);
        // ce.setType(ce.getAST().newSimpleType(ce.getAST().newName("")));
        return true;
    }

    // @thaddywu: support to automatically get the key length
    //   Currently, we only support the key as a string/integer, flattened tuple
    //   Return Tuple2(Tuple5(...), ..)
    // e.g. return new Tuple2(new Tuple5(i_item_id,i_item_desc,i_category,i_class,i_current_price),(cs_ext_sales_price));
    public int keyNumberOfElements = -1;
    public boolean visit(ReturnStatement node) {
        // System.out.println("\u001B[37m");
        // System.out.println("ReturnStatement");
        // System.out.println(node);
        // System.out.println("============");
        // System.out.println("\u001B[0m");

        if (node.getExpression() instanceof ClassInstanceCreation) {
            ClassInstanceCreation clc = (ClassInstanceCreation) node.getExpression();
            if (clc.getType().toString().equals("Tuple2")) {// return new Tuple2(.., ..)
                // for (Object arg: clc.arguments())
                //    System.out.println("arg:" + arg.toString());
                Expression arg0 = (Expression) clc.arguments().get(0);
                if (arg0 instanceof ClassInstanceCreation && ((ClassInstanceCreation) clc).getType().toString().startsWith("Tuple")) { // return new Tuple2(TupleX(), ..)
                    String arg0Type = ((ClassInstanceCreation) arg0).getType().toString();
                    keyNumberOfElements = Integer.valueOf(arg0Type.substring(5));
                    // System.out.println("keyLength: " + udfStack.peek() + " :" + String.valueOf(keyNumberOfElements));
                    String associated_udf = udfStack.peek();
                    if (log.keyLength.containsKey(associated_udf))
                        assert (log.keyLength.get(associated_udf) == keyNumberOfElements);
                    else
                        log.keyLength.put(associated_udf, keyNumberOfElements);
                }
                // else
                //    keyNumberOfElements = 1;

                
                // System.out.println(clc.toString());
                // System.out.println("key Length: " + String.valueOf(keyNumberOfElements));
            }
        }
        return true;
    }

    public boolean visit(MethodDeclaration node) {
        // System.out.println("\u001B[33m");
        // System.out.println("MethodDeclaration");
        // System.out.println(node);
        // System.out.println("============");
        // System.out.println("\u001B[0m");
        typeMapping = new HashMap<>();
        if (!inUDF) {
            // @thaddywu: filter out non-interesting method
            String name = node.getName().toString();
            Object returnType = node.getReturnType2();
            
            return name.equals("execute") ;//&& returnType != null && returnType.toString().equals("RDD");, the return type does not have to be RDD
        }
        SimpleName name = node.getName();
        // this.names.add(name.getIdentifier());
        methodDeclName = name.toString();

        // Check if the method declaration is for parameter overloading
        //Todo: Come up with a better fix // FIXME: 9/13/17
        if (node.getReturnType2() != null) {
            if (node.getReturnType2().toString().equals("Object")) {
                boolean vol = false;
                for (Modifier m : (List<Modifier>) node.modifiers()) {
                    if (m.isVolatile()) {
                        vol = true;
                    }
                }
                if (vol) return true;
            }
        }
        // log.loginfo(Logging.LogType.DEBUG, node.toString());

        Modifier mod = ((Modifier) node.modifiers().get(0));
        mod.setKeyword(Modifier.ModifierKeyword.STATIC_KEYWORD);
        FunctionStructure fs = new FunctionStructure(node.modifiers(), node.getReturnType2(), node.parameters(), node.getBody());
        // parameters = ((SingleVariableDeclaration) node.parameters().get(0)).getName().getIdentifier(); //@thaddywu: deprecated
        node.getBody().accept(this);
        fs.map = typeMapping;
        u_writer.enrollFunction(name.toString(), fs);//node.toString() + "\n  " );
        typeMapping = new HashMap<>();
        return false;
    }

    public boolean visit(MethodInvocation inv) {
        // System.out.println("\u001B[34m");
        // System.out.println("MethodInvocation");
        // System.out.println(inv);
        // System.out.println("============");
        // System.out.println("\u001B[0m");

        /* 
        if (call_graph.containsKey(methodDeclName)) {
            if (!methodDeclName.equals(inv.getName().toString()))
                call_graph.get(methodDeclName).add(inv.getName().toString());
        } else {
            ArrayList<String> temp = new ArrayList<String>();
            temp.add(inv.getName().toString());
            if (!methodDeclName.equals(inv.getName().toString()))
                call_graph.put(methodDeclName, temp);
        }
        */
        // @thaddywu: new implementation for function-level cfg in udf
        if (inUDF) {
            if (!call_graph.containsKey(methodDeclName))
                call_graph.put(methodDeclName, new ArrayList<String>());
            if (!call_graph.get(methodDeclName).contains(inv.getName().toString()) && !methodDeclName.equals(inv.getName().toString()))
                call_graph.get(methodDeclName).add(inv.getName().toString());
        }


        // System.out.println(inv.getName().getIdentifier());
        // System.out.println(inv.getExpression());
        // @thaddywu: m._1$mcI$sp() -> m._1()

        //if (inv.getName().getIdentifier().startsWith("_1")){
        //    inv.getName().setIdentifier("_1");
        //}
        //if (inv.getName().getIdentifier().startsWith("_2")){
        //    inv.getName().setIdentifier("_2");
        //}
        // @thaddywu kept, used to determine var type

        // @thaddywu: m._1() -> m_1 
        // However, this legacy code snippet is useless, as rewrite.rewriteAST() should be exectuted to activate this modification
        /* 
        if (inv.getName().getIdentifier().startsWith("_2") && parameters.equals(inv.getExpression().toString())) {
            SimpleName s = inv.getAST().newSimpleName(parameters+"_t2");
            rewrite.replace(inv , s,null);
            inv.getName().setIdentifier("_2");
        }
        if (inv.getName().getIdentifier().startsWith("_1") && parameters.equals(inv.getExpression().toString())) {
            //log.logdebug(inv.getName().getIdentifier());
            SimpleName s = inv.getAST().newSimpleName(parameters+"_t1");
           // rewrite.track(inv);
            rewrite.replace(inv , s,null);

            inv.getName().setIdentifier("_1");
        }*/

        if (Configuration.isSparkDataflowOperator(inv.getName().toString())) {
            ClassInstanceCreation cls = null;
            for (Object e: inv.arguments()) {
                if (!(e instanceof ClassInstanceCreation)) continue;
                if (((ClassInstanceCreation) e).getType().toString().equals("Serializable")) {
                    assert(cls == null); // can't have multiple udfs inside one operator
                    cls = (ClassInstanceCreation) e;
                }
            }

            if (cls == null) return true;

            inv.getExpression().accept(this);

            op_id += 1;
            String udfName = inv.getName().toString() + op_id; // @thaddywu: we change the place creating this udf name
            
            udfStack.add(udfName);
            startUDFClass(udfName);
            cls.accept(this);
            closeUDFClass();
            udfStack.pop();

            code2udfid.put(inv.toString(), udfName);
            
            String arg0 = getCorrespondingUDForInput(inv.getExpression());
            // System.out.println("DAG: " + udfName + ": " + arg0);
            ArrayList<String> argList = new ArrayList<String>();
            argList.add(arg0);
            log.dag.put(udfName, argList);
            return false;

            // .. .map(..) .map(arg1, arg2) 
            // @thaddywu: arg1, arg2 could be traversed multiple times,
            //  but only with invokeMethod set up as the wrapping method name, e.g. map,
            //  is meaningful.
            // @thaddywu: should have fixed it,
            //  set our own visit order for spark dataflow operator

        }
        if (inv.getName().toString().equals("join")) {
            // @thaddywu: new support for join operators,
            //  we add support for join, creat a new id for it
            //  in order to generate the DAG

            op_id += 1;
            String udfName = inv.getName().toString() + op_id; // @thaddywu: we change the place creating this udf name

            inv.getExpression().accept(this);
            List<Object> args = inv.arguments();
            assert(args.size() == 1); // we only accept one argument for join operator, otherwise this might not be a RDD join
            ASTNode joined = (ASTNode) args.get(0);

            udfStack.add(udfName);
            joined.accept(this);
            udfStack.pop();

            code2udfid.put(inv.toString(), udfName);

            String arg0 = getCorrespondingUDForInput(inv.getExpression());
            String arg1 = getCorrespondingUDForInput(joined);
            // System.out.println("DAG: " + udfName + ": " + arg0 + ", " + arg1);
            ArrayList<String> argList = new ArrayList<String>();
            argList.add(arg0);
            argList.add(arg1);
            log.dag.put(udfName, argList);
            return false;
        }

        // @thaddywu: support for join

        return true;
    }

    public boolean visit(VariableDeclarationStatement vds) {
        // System.out.println("\u001B[35m");
        // System.out.println("VariableDeclarationStatement");
        // System.out.println(vds);
        // System.out.println("============");
        // System.out.println("\u001B[0m");
        
        if (vds.getType().toString().equals("RDD")) {// RDD ~x = y.map..
            for (Object fg: vds.fragments())
            if (fg instanceof VariableDeclarationFragment) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) fg;
                vdf.getInitializer().accept(this);
                String varname = vdf.getName().getIdentifier().toString();
                String refTo = getCorrespondingUDForInput(vdf.getInitializer());
                code2udfid.put(varname, refTo);
                System.out.println(varname + " refersTo " + refTo);
            }
            else {
                ASTNode node = (ASTNode) fg;
                node.accept(this);
            }
            return false; // no multiple traversal
        }

        return true;
    }

    public void getAllCallee(String s, Set<String> callees) {
        // @thaddywu: obtain all reachable methods
        if (callees.contains(s)) return ;
        callees.add(s);
        ArrayList<String> funs = call_graph.get(s);
        if (funs != null)
        for (String fun: funs)
            getAllCallee(fun, callees);
    }


    public String getJPFFunction(String s) {
        // @thaddywu: recursively find the inner most apply function
        ArrayList<String> fun = call_graph.get(s);
        if (fun == null) return s;
        for (String funname : fun) {
            if (funname.contains("apply"))
                return getJPFFunction(funname);
        }
        return s;
    }
    //public void postVisit(ASTNode node) {
    //    System.out.println(node);
    //    System.out.println("nodeType" + node.getClass());
    //}
    UDFWriter u_writer;
    int op_id = 0;

    /*  @thaddywu: legacy uncalled function
    public void addToCallGraph(String caller, String callee) {
        ArrayList<String> temp = new ArrayList<String>();
        temp.add(callee);
        call_graph.put(caller, temp);
    }
    */

    //String target_func_jpf = null;

    //public void setTargetJPF(String fun) {
    //    target_func_jpf = fun;
    //}

    public void startUDFClass(String udfName) {
        // op_id += 1;
        //String class_name = invokedMethod + op_id;
        // u_writer = new UDFWriter(jpf_dir + class_name + ".java", Configuration.getArgs(class_name));
        u_writer = new UDFWriter(jpf_dir + udfName + ".java", null); // @thaddywu: argsToMain deprecated
        call_graph = new HashMap<>();
        // names = new HashSet();
        methodDeclName = "";
        //target_func_jpf = null;

        inUDF = true;
    }

    public void closeUDFClass() {
        Set<String> functions_set = new HashSet<>();
        String jpffunction = getJPFFunction("apply");
        getAllCallee(jpffunction, functions_set);
        
        String jpf_entry = u_writer.write(functions_set, jpffunction, this);
        u_writer.close();
        String new_classname = u_writer.filename.replace(".java", "");
        try {
            createJPFile(new_classname, jpf_entry, jpf_dir + new_classname + ".jpf", u_writer.isString , u_writer.symInputs);
            /*
            Runner.runCommand(new String[]{"javac", "-g",
                            Configuration.JPF_HOME + "jpf-symbc/src/examples/" + new_classname + ".java"},
                    Configuration.JAVA_RUN_DIR);
            */
            Runner.runCommand(new String[]{"javac", "-g", Runner.getModelPath() + new_classname + ".java"}, Runner.getModelPath());

            // log.jpf_dag.add(0, new JPFDAGNode(new_classname, jpf_dir + new_classname + ".jpf")); @thaddywu: deprecated
        } catch (Exception e) {
            e.printStackTrace();
        }

        // created by @thaddywu
        inUDF = false;
    }

    public void createJPFile(String target, String fun_name, String jpfPath, boolean isString , int numInputs) throws Exception {
        //if (target_func_jpf != null) {
        //    fun_name = target_func_jpf;
        //}
        String content = Configuration.JPF_FILE_PLACEHOLDER(target, fun_name, log.outputJava, isString , numInputs);
        FileWriter fw = null;
        try {
            File file = new File(jpfPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            fw = new FileWriter(file);

        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(fw);
            bw.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bw.close();
        }
    }
}