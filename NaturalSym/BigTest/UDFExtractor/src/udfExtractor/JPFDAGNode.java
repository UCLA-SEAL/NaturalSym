package udfExtractor;

import java.util.ArrayList;
import java.util.HashMap;

public class JPFDAGNode {
   public String operator_name ;
   // public String jpf_file; @deprecated
   public ArrayList<JPFDAGNode> parents;
   public int keyLength; // Sep 3, to record key length

   public JPFDAGNode(String op, String file, ArrayList<JPFDAGNode> par, int _keyLength){
      operator_name  = op;
      // jpf_file = file;
      parents = par;
      keyLength = _keyLength;
   }
}