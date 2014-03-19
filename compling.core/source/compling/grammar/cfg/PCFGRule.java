
package compling.grammar.cfg;

import java.util.*;


/**
 * A Simple class for a PCFG rule
 * 
 * @Author John Bryant
 * 
 */

public class PCFGRule extends CFGRule {

   double probability;


   public PCFGRule(String l, List<String> r, double probability) {
      super(l, r);
      this.probability = probability;
   }


   public PCFGRule(String l, String r, double probability) {
      super(l, r);
      this.probability = probability;
   }


   public double getProbability() {
      return probability;
   }


   public String toString() {
      return super.toString() + " : " + probability;
   }
}
