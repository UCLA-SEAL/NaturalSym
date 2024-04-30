package udfExtractor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

public class UDFDecompilerAndExtractor extends Logging {

    String classfile = null;
    // String classFile_jad = null;
    String jad_file = null;
    String outputJava = null;
    // ArrayList<JPFDAGNode> jpf_dag = new ArrayList<>(); @deprecated
    HashMap<String, ArrayList<String>> dag = new HashMap<>(); 
    HashMap<String, Integer> keyLength = new HashMap<>();


    public UDFDecompilerAndExtractor(String classf, String jad_f, String output_java) {
        classfile = classf;
        jad_file = jad_f;
        outputJava = output_java;
    }

    /* legacy code, not used as configuration is always read from files.    
    public JPFDAGNode getDAG() {
        
        JPFDAGNode prev = null;
        for(JPFDAGNode node  : jpf_dag ) {
            if(prev == null) {
                prev = node;
            }else {
                JPFDAGNode[] p = {prev};
                node.parents = p;
                prev = node;
            }
        }
        return prev;
    }     */
    public JPFDAGNode makeCFG(String node) { // convert string DAG to structured object
        ArrayList<JPFDAGNode> parents = new ArrayList<>();
        if (dag.containsKey(node)) {
            ArrayList<String> _parents = dag.get(node);
            for (String par: _parents) {
                parents.add(makeCFG(par));
                //if (node.startsWith("join") || node.startsWith("reduceByKey"))
                //    assert (par.startsWith("map") || par.startsWith("reduceByKey")); 
                    // We assume map is used before aggregate operators in that we do simple analysis to get the shape of keys.
                    //  @thaddy, Sep 3 2023
                    // Now, we let the 
            }
        }
        int keyNumberOfElements = -1;
        if (keyLength.containsKey(node))
            keyNumberOfElements = keyLength.get(node);
        if (node.startsWith("reduceByKey"))
            assert (keyNumberOfElements == 1 || keyNumberOfElements == -1);
        return new JPFDAGNode(node, node, parents, keyNumberOfElements);
     }
    public JPFDAGNode makeCFG() {
        for (String op: dag.keySet()) {
            for (String arg: dag.get(op))
                System.out.print(arg + ", ");
            System.out.print("-> " + op);
            if (keyLength.containsKey(op))
                System.out.print(" [#keys:" + keyLength.get(op) + "]");
            System.out.println();
        }
        Set<String> nonroots = new HashSet<String>();
        for (String op: dag.keySet())
            for (String arg: dag.get(op))
                nonroots.add(arg);
        String root = null;
        for (String op: dag.keySet())
            if (!nonroots.contains(op)) root = op;
        assert(root != null);
        assert(!root.startsWith("input"));
        return makeCFG(root);
    }
    
    public void parse(String str, String jpfdir) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        // @thaddywu: we should upgrade to AST.JLS8
        //  currently, main function is parsed as empty magically
        Document doc = new Document(str);
        parser.setSource(doc.get().toCharArray());
//        parser.setSource(str.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        AST ast = cu.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);
        cu.recordModifications();
        SparkProgramVisitor spv = new SparkProgramVisitor(this , jpfdir , rewriter);
        cu.accept(spv);
        // rewriter is not used at all
//       TextEdit edits = null;
//        edits = spv.rewrite.rewriteAST(doc, null);
//        try{edits.apply(doc);
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//        System.out.println(doc.get());

    }
    //read file content into a string
    public String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(10000); // @thaddywu: 1000->10000
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            //  System.out.println(numRead);
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString()
            .replaceAll("String\\.\\.MODULE" ,"String\\.MODULE")
            .replaceAll("BoxesRunTime\\.boxToInteger" , "");
    }

    //loop directory to get file list
    public void ParseFilesInDir(String jpfdir) throws Exception {
        if (new File(jad_file).exists()) {
            loginfo(LogType.INFO, "Deleting file " + jad_file + " ...");
            new File(jad_file).delete();
        }
        String[] args = new String[]{"jad", "-o", "-d", jpfdir, classfile + ".class"};
        // @thaddywu: -d specifys the output dir,
        // -o: silent overwrite
        runCommand(args);
        parse(readFileToString(jad_file), jpfdir);
    }

    public void runCommand(String[] args) {
        try {
            String s = "";
            for (String a : args) {
                s = s + "  " + a;
            }
            loginfo(LogType.INFO, "Running Command : " + s);
            Runtime runt = Runtime.getRuntime();
            Process p = runt.exec(s);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            // read the output from the command
            StringBuilder stdout = new StringBuilder("");
            while ((s = stdInput.readLine()) != null) {
                stdout.append(s);
            }
            loginfo(LogType.INFO, stdout.toString());
            StringBuilder stderr = new StringBuilder("");
            while ((s = stdError.readLine()) != null) {
                stderr.append(s);
            }
            loginfo(LogType.WARN, stderr.toString());
            stdError.close();
            stdInput.close();
        } catch (IOException e) {
            e.printStackTrace();
            assert (false);
        }
    }
}
