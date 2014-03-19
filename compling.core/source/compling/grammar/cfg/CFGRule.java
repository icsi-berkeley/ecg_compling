
package compling.grammar.cfg;

import java.util.*;
import compling.grammar.Rule;

/**
 * A Simple class for a CFG rule
 * 
 * @Author John Bryant
 * 
 */

public class CFGRule implements Rule<String>{

   String lhs;
   List<String> rhs;
    String id;

   public CFGRule(String lhs, List<String> rhs) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.id = this.lhs+"->"+rhs;
   }


   public CFGRule(String lhs, String rhs) {
      StringTokenizer st = new StringTokenizer(rhs);
      List<String> rhsList = new ArrayList<String>();
      while (st.hasMoreTokens()) {
         rhsList.add(st.nextToken());
      }
      this.lhs = lhs;
      this.rhs = rhsList;
      this.id = this.lhs+"->"+rhs;
   }


   public List<String> getRHS() {
      return rhs;
   }


   public String getLHS() {
      return lhs;
   }


   public String toString() {
       return id;
   }

    public String getRuleID(){return id;}
}
