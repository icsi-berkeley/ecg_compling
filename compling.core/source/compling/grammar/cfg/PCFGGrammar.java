
package compling.grammar.cfg;

import java.util.*;
import compling.util.*;


/**
 * A Simple class for a PCFG grammar
 * 
 * @Author John Bryant
 * 
 */

public class PCFGGrammar extends CFGGrammar<PCFGRule> {

   Counter<String> getLeftCornerSums(String lhs) {
      Counter<String> firstSymbolSum = new Counter<String>();
      for (CFGRule pr : getRules(lhs)) {
         firstSymbolSum.incrementCount(pr.getRHS().get(0), ((PCFGRule)pr).getProbability());
      }
      return firstSymbolSum;
   }


   public void addRule(PCFGRule r) {
      super.addRule(r);
   }


   HashMap<String, Counter<String>> getLeftCornerSums() {
      HashMap<String, Counter<String>> allLeftCornerSums = new HashMap<String, Counter<String>>();
      for (String symbol : nonTerminals) {
         allLeftCornerSums.put(symbol, getLeftCornerSums(symbol));
      }
      return allLeftCornerSums;
   }


   public static void main(String[] args) {
      PCFGGrammar g = new PCFGGrammar();
      g.addRule(new PCFGRule("S", "NP VP", .5));
      g.addRule(new PCFGRule("S", "VP", .2));
      g.addRule(new PCFGRule("S", "NP", .1));
      g.addRule(new PCFGRule("S", "NP VP PP", .2));
      g.addRule(new PCFGRule("NP", "Det N", .5));
      g.addRule(new PCFGRule("NP", "N", .3));
      g.addRule(new PCFGRule("NP", "Det Adj N", .2));
      System.out.println(g.getLeftCornerSums("S"));
      System.out.println(g.getLeftCornerSums("NP"));
      System.out.println(g.getLeftCornerSums());
   }

}
