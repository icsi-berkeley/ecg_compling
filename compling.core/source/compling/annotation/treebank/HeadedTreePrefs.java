
package compling.annotation.treebank;

import java.util.*;
//import java.io.*;
import compling.annotation.treebank.TreeBankSymbols;
import compling.grammar.cfg.*;


/**
 * This class uses the preferences defined by Collins in his dissertation for
 * finding lexical heads of Penn Treebank trees.
 * 
 * @author John Bryant
 */

public class HeadedTreePrefs implements HeadFinder {

   static final String L = "L";
   static final String R = "R";
   static boolean setup = false;
   public static boolean leftToRight = true;
   private static String X = R;
    //private static HeadedTree UNKNOWNTREE = new HeadedTree(TreeBankSymbols.UNKNOWN,TreeBankSymbols.UNKNOWN);
   // public static HeadedTreePrefs FINDER = new HeadedTreePrefs();

   static final String[] ADJPInfo = { L, TreeBankSymbols.NNS, TreeBankSymbols.QP,
         TreeBankSymbols.NN, TreeBankSymbols.DOLLAR, TreeBankSymbols.ADVP,
         TreeBankSymbols.JJ, TreeBankSymbols.VBN, TreeBankSymbols.VBG,
         TreeBankSymbols.ADJP, TreeBankSymbols.JJR, TreeBankSymbols.NP,
         TreeBankSymbols.JJS, TreeBankSymbols.DT, TreeBankSymbols.FW,
         TreeBankSymbols.RBR, TreeBankSymbols.RBS, TreeBankSymbols.SBAR,
         TreeBankSymbols.RB };
   static final String[] ADVPInfo = { R, TreeBankSymbols.RBR, TreeBankSymbols.RBS,
         TreeBankSymbols.FW, TreeBankSymbols.ADVP, TreeBankSymbols.TO,
         TreeBankSymbols.CD, TreeBankSymbols.JJR, TreeBankSymbols.JJ,
         TreeBankSymbols.IN, TreeBankSymbols.NP, TreeBankSymbols.JJS,
         TreeBankSymbols.NN };
   static final String[] CONJPInfo = { R, TreeBankSymbols.CC, TreeBankSymbols.RB,
         TreeBankSymbols.IN };
   static final String[] FRAGInfo = { R };
   static final String[] INTJInfo = { L };
   static final String[] LSTInfo = { R, TreeBankSymbols.LS, TreeBankSymbols.COLON };
   static final String[] NACInfo = { L, TreeBankSymbols.NN, TreeBankSymbols.NNS,
         TreeBankSymbols.NNP, TreeBankSymbols.NNPS, TreeBankSymbols.NP,
         TreeBankSymbols.NAC, TreeBankSymbols.EX, TreeBankSymbols.DOLLAR,
         TreeBankSymbols.CD, TreeBankSymbols.QP, TreeBankSymbols.PRP,
         TreeBankSymbols.VBG, TreeBankSymbols.JJ, TreeBankSymbols.JJS,
         TreeBankSymbols.JJR, TreeBankSymbols.ADJP, TreeBankSymbols.FW };
   static final String[] PPInfo = { R, TreeBankSymbols.IN, TreeBankSymbols.TO,
         TreeBankSymbols.VBG, TreeBankSymbols.VBN, TreeBankSymbols.RP,
         TreeBankSymbols.FW };
   static final String[] PRNInfo = { L };
   static final String[] ROOTInfo = { L };
   static final String[] PRTInfo = { R, TreeBankSymbols.RP };
   static final String[] QPInfo = { L, TreeBankSymbols.DOLLAR, TreeBankSymbols.IN,
         TreeBankSymbols.NNS, TreeBankSymbols.NN, TreeBankSymbols.JJ,
         TreeBankSymbols.RB, TreeBankSymbols.DT, TreeBankSymbols.CD,
         TreeBankSymbols.NCD, TreeBankSymbols.QP, TreeBankSymbols.JJR,
         TreeBankSymbols.JJS };
   static final String[] RRCInfo = { R, TreeBankSymbols.VP, TreeBankSymbols.NP,
         TreeBankSymbols.ADVP, TreeBankSymbols.ADJP, TreeBankSymbols.PP };
   static final String[] SInfo = { L, TreeBankSymbols.TO, TreeBankSymbols.IN,
         TreeBankSymbols.VP, TreeBankSymbols.S, TreeBankSymbols.SBAR,
         TreeBankSymbols.ADJP, TreeBankSymbols.UCP, TreeBankSymbols.NP };
   static final String[] SBARInfo = { L, TreeBankSymbols.WHNP, TreeBankSymbols.WHPP,
         TreeBankSymbols.WHADVP, TreeBankSymbols.WHADJP, TreeBankSymbols.IN,
         TreeBankSymbols.DT, TreeBankSymbols.S, TreeBankSymbols.SQ,
         TreeBankSymbols.SINV, TreeBankSymbols.SBAR, TreeBankSymbols.FRAG };
   static final String[] SBARQInfo = { L, TreeBankSymbols.SQ, TreeBankSymbols.S,
         TreeBankSymbols.SINV, TreeBankSymbols.SBARQ, TreeBankSymbols.FRAG };
   static final String[] SINVInfo = { L, TreeBankSymbols.VBZ, TreeBankSymbols.VBD,
         TreeBankSymbols.VBP, TreeBankSymbols.VB, TreeBankSymbols.MD,
         TreeBankSymbols.VP, TreeBankSymbols.S, TreeBankSymbols.SINV,
         TreeBankSymbols.ADJP };
   static final String[] SQInfo = { L, TreeBankSymbols.VBZ, TreeBankSymbols.VBD,
         TreeBankSymbols.VBP, TreeBankSymbols.VB, TreeBankSymbols.MD,
         TreeBankSymbols.VP, TreeBankSymbols.SQ };
   static final String[] UCPInfo = { R };
   static final String[] VPInfo = { L, TreeBankSymbols.TO, TreeBankSymbols.VBD,
         TreeBankSymbols.VBN, TreeBankSymbols.MD, TreeBankSymbols.VBZ,
         TreeBankSymbols.VB, TreeBankSymbols.VBG, TreeBankSymbols.VBP,
         TreeBankSymbols.VP, TreeBankSymbols.ADJP, TreeBankSymbols.NN,
         TreeBankSymbols.NNS, TreeBankSymbols.NP };
   static final String[] WHADJPInfo = { L, TreeBankSymbols.CC, TreeBankSymbols.WRB,
         TreeBankSymbols.JJ, TreeBankSymbols.ADJP };
   static final String[] WHADVPInfo = { R, TreeBankSymbols.CC, TreeBankSymbols.WRB };
   static final String[] WHNPInfo = { L, TreeBankSymbols.WDT, TreeBankSymbols.WP,
         TreeBankSymbols.WPDOLLAR, TreeBankSymbols.WHADJP, TreeBankSymbols.WHPP,
         TreeBankSymbols.WHNP };
   static final String[] WHPPInfo = { R, TreeBankSymbols.IN, TreeBankSymbols.TO,
         TreeBankSymbols.FW };

   static final String[][] NPInfo = {
         { R, TreeBankSymbols.NN, TreeBankSymbols.NNP, TreeBankSymbols.NNPS,
               TreeBankSymbols.NNS, TreeBankSymbols.NX, TreeBankSymbols.POS,
               TreeBankSymbols.JJR },
         { L, TreeBankSymbols.NP },
         { R, TreeBankSymbols.DOLLAR, TreeBankSymbols.ADJP, TreeBankSymbols.PRN },
         { R, TreeBankSymbols.CD },
         { R, TreeBankSymbols.JJ, TreeBankSymbols.JJS, TreeBankSymbols.RB,
               TreeBankSymbols.QP } };

   static HashMap<String, String[]> SymbolToHeadPrefs = new HashMap<String, String[]>();


   /*
    * public static String[] ALLSYMBOLS = {TreeBankSymbols.ADJP,
    * TreeBankSymbols.ADVP, TreeBankSymbols.CONJP, TreeBankSymbols.FRAG,
    * TreeBankSymbols.INTJ, TreeBankSymbols.LST, TreeBankSymbols.NAC,
    * TreeBankSymbols.NP, TreeBankSymbols.PP, TreeBankSymbols.PRN,
    * TreeBankSymbols.QP, TreeBankSymbols.RRC, TreeBankSymbols.S,
    * TreeBankSymbols.SBAR, TreeBankSymbols.SBARQ, TreeBankSymbols.SINV,
    * TreeBankSymbols.SQ, TreeBankSymbols.UCP, TreeBankSymbols.VP,
    * TreeBankSymbols.WHADJP, TreeBankSymbols.WHADVP, TreeBankSymbols.WHNP,
    * TreeBankSymbols.WHPP};
    */
   static public void init() {
      SymbolToHeadPrefs.put(TreeBankSymbols.ADJP, ADJPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.ADVP, ADVPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.CONJP, CONJPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.FRAG, FRAGInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.INTJ, INTJInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.LST, LSTInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.NAC, NACInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.PP, PPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.PRN, PRNInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.PRT, PRTInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.QP, QPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.RRC, RRCInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.S, SInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.SBAR, SBARInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.SBARQ, SBARQInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.SINV, SINVInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.SQ, SQInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.UCP, UCPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.VP, VPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.WHADJP, WHADJPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.WHADVP, WHADVPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.WHNP, WHNPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.WHPP, WHPPInfo);
      SymbolToHeadPrefs.put(TreeBankSymbols.ROOT, ROOTInfo);
      setup = true;
      if (!leftToRight) {
         X = L;
      }
   }


   // Assumes that children is an array of HeadedTree
   public HeadedTree findHead(String symbol, List children) {
      if (!setup) {
         init();
      }
      if (symbol.indexOf('-') != -1) {
         symbol = symbol.substring(0, symbol.indexOf('-'));
      }
      if (children.size() == 1) {
         return ((HeadedTree) children.get(0));
      } else {
         String[] prefs = (String[]) SymbolToHeadPrefs.get(symbol);
         if (symbol.equals(TreeBankSymbols.NP)
               || symbol.equals(TreeBankSymbols.NPTMP)
               || symbol.equals(TreeBankSymbols.NX)
               || symbol.equals(TreeBankSymbols.X)) {
            if (((HeadedTree) children.get(children.size() - 1)).getLabel().equals(
                  TreeBankSymbols.POS)) {
               return ((HeadedTree) children.get(children.size() - 1));
            }
            for (int h = 0; h < NPInfo.length; h++) {
               prefs = NPInfo[h];
               String direction = prefs[0];
               int adjustment = 0;
               int polarity = 1;
               if (direction.equals(X)) {
                  adjustment = children.size() - 1;
                  polarity = -1;
               }
               for (int j = 0; j < children.size(); j++) {
                  HeadedTree child = (HeadedTree) children.get(j * polarity
                        + adjustment);
                  for (int i = 1; i < prefs.length; i++) {
                     if (prefs[i].equals(child.getLabel())) {
                        return child;
                     }
                  }
               }
            }
            return ((HeadedTree) children.get(children.size() - 1));
         } else if (prefs == null) {
            System.out.println("Symbol " + symbol + " has a null prefs");
            printChildren(children);
	    throw new RuntimeException("Unknown tree symbol in HeadedTreePrefs: "+symbol); 
            //return null;

         } else if (prefs.length == 1) { // just a direction
            String direction = prefs[0];
            int index = 0;
            if (direction.equals(X)) {
               index = children.size() - 1;
            }
            return ((HeadedTree) children.get(index));
         } else {
            String direction = prefs[0];
            int adjustment = 0;
            int polarity = 1;
            if (direction.equals(X)) {
               adjustment = children.size() - 1;
               polarity = -1;
            }
            for (int i = 1; i < prefs.length; i++) {
               for (int j = 0; j < children.size(); j++) {
                  HeadedTree child = (HeadedTree) children.get(j * polarity
                        + adjustment);
                  if (prefs[i].equals(child.getLabel())) {
                     return child;
                  }
               }
            }
            // no matching children found, so just take the first (or last)
            // child
            int index = 0;
            if (direction.equals(X)) {
               index = children.size() - 1;
            }
            return ((HeadedTree) children.get(index));
         }
      }
   }


   static void printChildren(List children) {
      System.out.print("Children: ");
      for (int i = 0; i < children.size(); i++) {
         System.out.print(((Tree) children.get(i)).getLabel() + " ");
      }
      System.out.println(";");
   }
}
