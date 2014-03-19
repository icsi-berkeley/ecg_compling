
package compling.parser.treebank;

import compling.utterance.*;
import compling.util.*;
import compling.annotation.treebank.*;
import compling.grammar.cfg.*;

import java.util.*;


public class TreeBankPCFGMaker {

   public static PCFGGrammar makePCFG(TreeBankFileIterator tbfi) {
      HashMap<String, Counter<String>> lhsToCounter = new HashMap<String, Counter<String>>();
      while (tbfi.hasNext()) {
         Sentence s = tbfi.next();
         TreeBankAnnotation tba = (TreeBankAnnotation) s
               .getAnnotation(UtteranceAnnotation.PENNTREEBANK);
         List<String> ruleStrings = new ArrayList<String>();
         makeRuleStrings(tba.getNormalizedTree(), ruleStrings);
         for (String ruleString : ruleStrings){
            String lhs = getLHS(ruleString);
            String rhs = getRHS(ruleString);
            Counter<String> lhsCounter = lhsToCounter.get(lhs);
            if (lhsCounter == null) {
               lhsCounter = new Counter<String>();
               lhsToCounter.put(lhs, lhsCounter);
            }
            lhsCounter.incrementCount(rhs, 1.0);
         }
      }
      PCFGGrammar grammar = new PCFGGrammar();
      for (String lhs : lhsToCounter.keySet()){
         Counter<String> lhsCounter = lhsToCounter.get(lhs);
         double normalizer = lhsCounter.totalCount();
         for (String rhs : lhsCounter.keySet()){
            double count = lhsCounter.getCount(rhs);
            grammar.addRule(new PCFGRule(lhs, rhs, count / normalizer));
         }
      }
      return grammar;
   }


   private static void makeRuleStrings(Tree t, List<String> l) {
      if (t.isLeaf() || t.isPreTerminal()) {
         return;
      }
      StringBuffer ruleString = new StringBuffer();
      ruleString.append(t.getLabel()).append(" > ");
      for (Iterator i = t.getChildren().iterator(); i.hasNext();) {
         Tree child = (Tree) i.next();
         makeRuleStrings(child, l);
         ruleString.append(child.getLabel()).append(" ");
      }
      l.add(ruleString.toString().trim());
   }


   private static String getLHS(String ruleString) {
      return ruleString.substring(0, ruleString.indexOf(">")).trim();
   }


   private static String getRHS(String ruleString) {
      return ruleString.substring(ruleString.indexOf(">")).trim();
   }


   public static void main(String args[]) {
      System.out.println(TreeBankPCFGMaker.makePCFG(new TreeBankFileIterator("wsj",
            200, 200)));
   }

}
