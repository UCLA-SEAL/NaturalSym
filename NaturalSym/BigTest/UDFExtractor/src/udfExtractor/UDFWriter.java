package udfExtractor;

import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Comparator;


public class UDFWriter {
    String filename = null;
    BufferedWriter bw = null;
    HashMap<String, FunctionStructure> functions = new HashMap<>();
    String argsToMain = null; // @thaddywu: deprecated
    boolean isString = false;


    public UDFWriter(String filen, String argsmain) {
        //argsToMain = argsmain;
        try {
            filename = filen.replace("$", "");
            String arr[] = filename.split("/");
            String file_name = arr[arr.length - 1];
            File file = new File(filename);
            filename = file_name;
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            // bw.write(skeleton);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void enrollFunction(String name, FunctionStructure code) {
        functions.put(name, code);
    }

    /* 
    public String getInputParamters(String f_name) {
        FunctionStructure fs = functions.get(f_name);
        String s = "";
        for (Object par : fs.parameters) {
            SingleVariableDeclaration p = (SingleVariableDeclaration) par;
            s = s + par + ",";
        }
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
    public String getInputParamtersName(String f_name) {
        FunctionStructure fs = functions.get(f_name);
        String s = "";
        for (Object par : fs.parameters) {
            SingleVariableDeclaration p = (SingleVariableDeclaration) par;
            s = s + ((SingleVariableDeclaration) par).getName().getIdentifier() + ",";
        }
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
    public String getReturnType(String f_name) {
        FunctionStructure fs = functions.get(f_name);

        return fs.returnType.toString().equals("ArrayOps")?"Object[]":fs.returnType.toString();
    }
    */
    
    public String write(Set<String> used_func, String target_func, SparkProgramVisitor visitor) {
        String wrapper_func_body = "";
        String wrapper_name = "applyReduce";
        // isString = argsToMain.startsWith("\"") && argsToMain.endsWith("\"");
        isString = true;

        String jpf_entry = target_func;
        String content = "public class " + filename.replace(".java", "") + " {\n";
        String argsToMain = ""; // @thaddywu: created from entry function plain code
        for (String fun : used_func) {
            if (functions.containsKey(fun)) {
                String fun_code = getFunctionCode(fun);
                if (fun.equals(target_func)) { // @thaddywu, automate driver parameter generation
                    int l_par = fun_code.indexOf("(");
                    int r_par = fun_code.indexOf(")");
                    String[] argList = fun_code.substring(l_par + 1, r_par).split(",");
                    for (String arg: argList) {
                        arg = arg.trim();
                        if (arg.startsWith("int")) argsToMain += "1,";
                        else if (arg.startsWith("String")) argsToMain += "\"1\",";
                        else if (arg.startsWith("String[]")) argsToMain += "{\"1\",\"1\"},";
                        else if (arg.startsWith("int[]")) argsToMain += "{1,2,3,4},";
                        else assert(false);
                    }
                    if (argsToMain.endsWith(",")) argsToMain = argsToMain.substring(0, argsToMain.length() - 1);
                }
                
                // This is used to deal with returned tuple
                String helper = filename.replace(".java", ""); // udf name
                boolean containTupleOutput = false ;
                for (int i = 2; i <= 5; i++) {
                    String Tuplei = "Tuple" + String.valueOf(i);
                    if (fun_code.contains(Tuplei)) {
                        containTupleOutput = true;
                        fun_code = fun_code.replace(Tuplei, helper);
                    }
                }
                content += fun_code;
                if (containTupleOutput)
                    content += replaceTuples(helper);
                /* 
                @thaddywu: used to only support tuple2
                if (fun_code.contains("Tuple2")) {
                    fun_code = fun_code.replaceAll("Tuple2", filename.replace(".java", ""));
                    content += fun_code;
                    content += replaceTuple2(filename.replace(".java", ""));

                } else
                    content += fun_code;
                */
            }
        }

        String method_call = target_func + "(" + argsToMain + ");\n";
        if (filename.startsWith("reduce")) {
            wrapper_name = "applyReduce";
            wrapper_func_body = "\tstatic int " + wrapper_name + "(int[] a) {\n" +
                    "\t\tint s = a[0];\n" +
                    "\t\tfor(int i = 1 ; i < " + Runner.loop_bound() + " ; i++){\n" + //// This is where we set the upper bound for the loop in reduce.
                    "\t\t\ts = " + target_func + "(s , a[i]);\n" +
                    "\t\t}\n" +
                    "\t\treturn s;\n" +
                    "\t}\n";
            jpf_entry = wrapper_name;
            target_func = wrapper_name;
            argsToMain = "{1,2,3,4}";
            method_call = "int[] arr = " + argsToMain + ";\n\t\t" + target_func + "(arr);\n";
            content += wrapper_func_body;
        }
        /*else if (filename.startsWith("flatMap")) {
            wrapper_name = "applyFlatmap";
            String retType = getReturnType(target_func);
            if(retType.endsWith("[]")) retType = retType.replace("[]", "");
            wrapper_func_body = "static " + retType +" "+  wrapper_name + "( "+ getInputParamters(target_func)+" ) {\n" +
                    "   return " +target_func+"(" + getInputParamtersName(target_func) +")["+ Runner.loop_bound + "];\n"+
                    "}\n";
            visitor.setTargetJPF(wrapper_name);
            target_func = wrapper_name;
            method_call =  target_func + "("+argsToMain+");\n";
        }*/

        content += 
            "\tpublic static void main(String[] args) { \n" +
            "\t\t" + method_call +
            "\t}\n";

        try {
            bw.write(content);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return jpf_entry;
    }

    public void close() {
        try {
            if (bw != null)
                bw.write("}");
            bw.close();
        } catch (Exception ex) {
            System.out.println("Error in closing the BufferedWriter" + ex);
        }
    }

    int wrapNull = 0;
    int symInputs = 0;
    String body_str ;

    public String getFunctionCode(String name) {
        FunctionStructure fs = functions.get(name);
        String s = "";
        for (Modifier m : fs.mods) {
            s = s + " " + m.getKeyword().toString();
        }

        if(fs.returnType.toString().equals("ArrayOps")){
            s = s + " " + "Object[]";
        }else {
            s = s + " " + fs.returnType.toString();
        }
        s = s + " " + name + "(";
        /* 
        if (name.equals("apply")) {
            System.out.println("");
        }*/


        body_str = fs.body.toString();
        //for (String ty : fs.map.keySet()) {
        //    String typ = fs.map.get(ty);
        //    body_str = body_str.replaceAll("\\Q(" + typ + ")\\E", "");
        //}
        body_str = body_str.replaceAll("\\QPredef$.MODULE$.refArrayOps\\E" , "");
        body_str = body_str.replaceAll("ArrayOps" , "Object[]");
        // @thaddywu: moved out
        // body_str = body_str.replaceAll("\\QPredef$.MODULE$.augmentString\\E" , "");
        // body_str = body_str.replaceAll("\\QStringOps\\E" , "String");
        // body_str = body_str.replaceAll("\\(\\(new StringOps\\(Predef\\$\\.MODULE\\$\\.augmentString\\(([^)]*?)\\)\\)\\)\\.toInt\\(\\)\\)", "Integer.parseInt($1)");
        body_str = body_str.replace("(new StringOps(Predef$.MODULE$.augmentString(", "Integer.parseInt(");
        body_str = body_str.replace("))).toInt()", ")");
        // ((new StringOps(Predef$.MODULE$.augmentString(str))).toInt()) -> Integer.parseInt($1)

        for (Object par : fs.parameters) {
            SingleVariableDeclaration p = (SingleVariableDeclaration) par;
            String para = getParameterType(p, fs);

            s = s + para + ",";
        }
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        s = s + ")"; //+ fs.body.toString();
//        if (wrapNull > 0) {
//            body_str = wrapNullAroundBody(body_str, fs.returnType.toString(), ((SingleVariableDeclaration) fs.parameters.get(0)).getName().getIdentifier(), wrapNull);
//        }

        
        // @thaddywu: add new replacement
        body_str = tupleRenaming(body_str);
        return s + body_str;
    }

    HashMap<String, String> replacements = new HashMap<>();

    private String tupleRenaming(String code) {
        Set<String> sortSet = new TreeSet<String>(Comparator.reverseOrder());
        sortSet.addAll(replacements.keySet());
        for (String k : sortSet) {
            code = code.replaceAll(k, replacements.get(k));
        }
        return code;
    }
    //@thaddywu: rewrite the transformation rule
    /* 
    private String dirtyTransform(String par_name, String new_name, String id) {
        // System.out.println("dirty: " + par_name + " | " + new_name + " | " + id);
        // System.out.println(body_str);
        
        if (body_str.contains("(String)" + par_name + "._" + id + "()")) {
            body_str = body_str.replace("(String)" + par_name + "._" + id + "()", new_name);
            return "String";
        }
        if (body_str.contains(par_name + "._" + id + "$mcI$sp()")) {
            body_str = body_str.replace(par_name + "._" + id + "$mcI$sp()", new_name);
            return "int";
        }
        if (body_str.contains(par_name + "._" + id + "()")) {
            assert false: body_str + "\n" + par_name + "._" + id + "()";
            body_str = body_str.replace(par_name + "._" + id + "()", new_name);
            return "int"; // @thaddywu: funky!
        }
        
        return "int"; //@thaddywu: require unused component must be integer
    }
    */
    /*
    private String getParameterType(SingleVariableDeclaration p, FunctionStructure fs) {
        String par_name = p.getName().getIdentifier();
        // System.out.println(par_name + " -> " + p.getType().toString());
        if (p.getType().toString().startsWith("Tuple2")) {
            //_1$mcI$sp: int; _1 int
            String type_t1 = dirtyTransform(par_name, par_name + "_t1", "1");
            String type_t2 = null;
            String type_t3 = null;

            if (body_str.contains("((Tuple2)" + par_name + "._2())")) {
                type_t2 = dirtyTransform("((Tuple2)" + par_name + "._2())", par_name + "_t2", "1");
                type_t3 = dirtyTransform("((Tuple2)" + par_name + "._2())", par_name + "_t3", "2");
            }
            else {
                type_t2 = dirtyTransform(par_name, par_name + "_t2", "2");
            }

            symInputs = (type_t3 != null) ? 3 : 2;
            String result = "";
            result += type_t1 + " " + par_name + "_t1";
            result += ", ";
            result += type_t2 + " " + par_name + "_t2";

            if (type_t3 != null) {
                result += ", ";
                result += type_t3 + " " + par_name + "_t3";
            }
            return result;
        } else {
            return p.toString();
        }
    } */
    Vector<String> types;
    String _par_name;
    private String getStringExpression(String par_name, int id) {
        return "(String)" + par_name + "._" + String.valueOf(id) + "()";
    }
    private String getIntExpression(String par_name, int id) {
        return par_name + "._" + String.valueOf(id) + "$mcI$sp()";
    }
    private String getTupleNExpression(String par_name, int id, int N) {
        return "((Tuple" + String.valueOf(N) + ")" + par_name + "._" + String.valueOf(id) + "())";
    }
    private void guessType(String par_name, int id) {
        String v = "n/a";
        if (body_str.contains(v=getStringExpression(par_name, id)))
            {flatternTupleInput(v, "String"); return ;}
        if (body_str.contains(v=getIntExpression(par_name, id)))
            {flatternTupleInput(v, "int"); return ;}
        for (int k = 2; k <= 5; k++)
            if (body_str.contains(v=getTupleNExpression(par_name, id, k)))
                {flatternTupleInput(v, "Tuple" + String.valueOf(k)); return ;}
        
        if (!body_str.contains(par_name + "._" + String.valueOf(id))) {
            // We assume this part is a integer as it's not used in the analyzed UDF
            // However, when it's a tuple, we will fail to generate the correct number
            //  aux variables, leading to alignment problem.
            flatternTupleInput(par_name + "._" + String.valueOf(id), "String");
            return ;
        }
        if (body_str.contains(".append(" + par_name + "._" + String.valueOf(id) + "())")) {
            // special case: In string builder, (String) conversion is not be explict.
            //  .scala row._1 + row._2 + row._3
            //  .java  ( new StringBuilder()).append((String)row._1()).append(row._2()).append(row._3())
            flatternTupleInput(par_name + "._" + String.valueOf(id) + "()", "String");
            return ;
        }
            
        // There's a chance we didn't correctly parse the string
        assert false: body_str + "\n" + "Couldn't guess the type of " + par_name + "._" + String.valueOf(id);
    }
    private void flatternTupleInput(String par_name, String par_type) {
        // Recursive procedure to expand tuple
        // System.out.println(par_name + " | " + par_type);
        if (par_type.startsWith("Tuple")) {
            int tuple_n = Integer.parseInt(par_type.substring(5));
            for (int i = 1; i <= tuple_n; i++)
                guessType(par_name, i);
        }
        else {
            // rewrite par_name to a neat way, we expand tuples to extend the capability of SymExec.
            types.add(par_type);
            symInputs += 1;
            String new_name = _par_name + "_t" + String.valueOf(symInputs);
            body_str = body_str.replace(par_name, new_name);
        }
    }
    private String getParameterType(SingleVariableDeclaration p, FunctionStructure fs) {
        String par_name = p.getName().getIdentifier();
        // System.out.println(par_name + " -> " + p.getType().toString());
        if (p.getType().toString().startsWith("Tuple")) {
            //_1$mcI$sp: int; _1 int
            types = new Vector<String>();
            _par_name = par_name;
            symInputs = 0;
            // @thaddywu: We must pass the type of par_name,
            //  because par_name is of type TupleN, there's no explicit conversion for par_name
            flatternTupleInput(par_name, p.getType().toString());
            
            String result = "";
            for (int i = 0; i < symInputs; i++) {
                if (i != 0) result += ", ";
                result += types.get(i) + " " + par_name + "_t" + String.valueOf(i+1);
            }
            return result;
        } else {
            return p.toString();
        }
    }
    
    String replaceTuples(String s) {
        String template = 
    // "    String s1, s2, s3, s4, s5, s6;\n" + 
    // "    int i1, i2;\n" + 
    // "    helper o1, o2;\n" + 
    // order matters! @thaddywu: ReferenceFieldInfo relies on the order to flatten tuples.
    "    String s1; int i1; helper o1;\n" + 
    "    String s2; int i2; helper o2;\n" +
    "    String s3, s4, s5, s6;\n" +
    "    public helper(String x1, String x2) {s1 = x1; s2 = x2;}\n" + 
    "    public helper(String x1, String x2, String x3) {s1 = x1; s2 = x2; s3 = x3;}\n" + 
    "    public helper(String x1, String x2, String x3, String x4) {s1 = x1; s2 = x2; s3 = x3; s4 = x4;}\n" + 
    "    public helper(String x1, String x2, String x3, String x4, String x5) {s1 = x1; s2 = x2; s3 = x3; s4 = x4; s5 = x5;}\n" + 
    "    public helper(int x1, String x2) {i1 = x1; s2 = x2;}\n" + 
    "    public helper(int x1, int x2) {i1 = x1; i2 = x2;}\n" +
    "    public helper(int x1, helper x2) {i1 = x1; o2 = x2;}\n" +
    "    public helper(String x1, int x2) {s1 = x1; i2 = x2;}\n" +  
    "    public helper(String x1, helper x2) {s1 = x1; o2 = x2;}\n" +
    "    public helper(helper x1, String x2) {o1 = x1; s2 = x2;}\n" + 
    "    public helper(helper x1, int x2) {o1 = x1; i2 = x2;}\n" +
    "    public helper(helper x1, helper x2) {o1 = x1; o2 = x2;}\n"
        ;
        return template.replace("helper", s);
    }
    /*
    String replaceTuple2(String s) {
        return  "\tString sa,sb;\n" +
                "\tint ia,ib;\n" +
                "\tpublic int _1(){\n" +
                "\t\treturn ia;\n" +
                "\t}\n" +
                "\tpublic int _2(){\n" +
                "\t\treturn ib;\n" +
                "\t}\n" +
                "\tpublic " + s + "(int k, int v){\n" +
                "\t\tia = k;\n" +
                "\t\tib = v;\n" +
                "\t}\n" +
                "\tpublic " + s + "(String k, int v){\n" +
                "\t\tsa = k;\n" +
                "\t\tib = v;\n" +
                "\t}\n" +
                "\tpublic " + s + "(int k, String v){\n" +
                "\t\tia = k;\n" +
                "\t\tsb = v;\n" +
                "\t}\n" +
                "\tpublic " + s + "(String k, String v){\n" +
                "\t\tsa = k;\n" +
                "\t\tsb = v;\n" +
                "\t}\n";
    }
    */
}
