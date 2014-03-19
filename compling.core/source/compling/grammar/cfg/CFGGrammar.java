
package compling.grammar.cfg;

import java.util.*;
import compling.grammar.*;

/**
 * A Simple class for a CFG grammar
 * 
 * @Author John Bryant
 * 
 */

public class CFGGrammar<RULE extends CFGRule> implements Grammar<String, RULE>{

   HashMap<String, List<RULE>> leftHandSides = new HashMap<String, List<RULE>>();
   HashSet<String> nonTerminals = new HashSet<String>();
   HashSet<String> allSymbols = new HashSet<String>();
    static final String STARTSYMBOL = "S";

   public void addRules(List<RULE> rules) {
      for (RULE rule : rules) addRule(rule);
   }


   public void addRule(RULE r) {
       System.out.println(r);
      List<RULE> ruleList = leftHandSides.get(r.getLHS());
      if (ruleList == null) {
         ruleList = new ArrayList<RULE>();
         leftHandSides.put(r.getLHS(), ruleList);
      }
      ruleList.add(r);
      nonTerminals.add(r.getLHS());
      allSymbols.addAll(r.getRHS());
      allSymbols.add(r.getLHS());
   }


   public List<RULE> getRules(String lhs) {
      return leftHandSides.get(lhs);
   }


   public boolean isNonTerminal(String symbol) {
      return nonTerminals.contains(symbol);
   }


   public HashSet<String> getAllSymbols() {
      return allSymbols;
   }

    public boolean isStartSymbol(String sym){return sym.equals(STARTSYMBOL);}

    public String getStartSymbol(){return STARTSYMBOL;}


   public String toString() {
      StringBuffer sb = new StringBuffer();
      for (String lhs : leftHandSides.keySet()) {
         List<? extends RULE> ruleList = leftHandSides.get(lhs);
         sb.append(ruleList).append("\n");
      }
      return sb.toString();
   }

}
