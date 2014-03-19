
package compling.annotation.treebank;

import java.util.*;


/**
 * This file is a place to store symbols associated with the Penn Treebank.
 * 
 * @author John Bryant
 * 
 */

public class TreeBankSymbols {

   public static final String ROOT = "ROOT";
   public static final String UNKNOWN = "*UNKNOWN*";
   public static final String NCD = "NCD";
   public static final String NPTMP = "NPTMP";
   public static final String NX = "NX";
   public static final String X = "X";
   public static final String ADJP = "ADJP";
   public static final String ADVP = "ADVP";
   public static final String CONJP = "CONJP";
   public static final String FRAG = "FRAG";
   public static final String INTJ = "INTJ";
   public static final String LST = "LST";
   public static final String NAC = "NAC";
   public static final String PP = "PP";
   public static final String PRN = "PRN";
   public static final String PRT = "PRT";
   public static final String QP = "QP";
   public static final String RRC = "RRC";
   public static final String S = "S";
   public static final String SBAR = "SBAR";
   public static final String SBARQ = "SBARQ";
   public static final String SINV = "SINV";
   public static final String SQ = "SQ";
   public static final String UCP = "UCP";
   public static final String VP = "VP";
   public static final String WHADJP = "WHADJP";
   public static final String WHADVP = "WHADVP";
   public static final String WHNP = "WHNP";
   public static final String WHPP = "WHPP";
   public static final String NNS = "NNS";
   public static final String NN = "NN";
   public static final String NNPS = "NNPS";
   public static final String NNP = "NNP";
   public static final String DOLLAR = "$";
   public static final String JJ = "JJ";
   public static final String VBN = "VBN";
   public static final String VBG = "VBG";
   public static final String VBZ = "VBZ";
   public static final String VBP = "VBP";
   public static final String VBD = "VBD";
   public static final String VB = "VB";
   public static final String MD = "MD";
   public static final String JJR = "JJR";
   public static final String NP = "NP";
   public static final String DT = "DT";
   public static final String JJS = "JJS";
   public static final String FW = "FW";
   public static final String RBR = "RBR";
   public static final String RBS = "RBS";
   public static final String RB = "RB";
   public static final String TO = "TO";
   public static final String CD = "CD";
   public static final String IN = "IN";
   public static final String LS = "LS";
   public static final String COLON = ":";
   public static final String EX = "EX";
   public static final String PRP = "PRP";
   public static final String RP = "RP";
   public static final String WRB = "WRB";
   public static final String CC = "CC";
   public static final String WDT = "WDT";
   public static final String WP = "WP";
   public static final String WPDOLLAR = "WP$";
   public static final String POS = "POS";
   public static final String TRACE = "TRACE";
   public static final String PDT = "PDT";
   public static final String PRPDOLLAR = "PRP$";
   public static final String SYM = "SYM";
   public static final String UH = "UH";
   public static final String LQUOTE = "LQ"; // this is not consistent with the
                                             // treebank trees
   public static final String RQUOTE = "RQ"; // this is not consistent with the
                                             // treebank trees
   public static final String LPAREN = "LPAREN"; // this is not consistent
                                                   // with the treebank trees
   public static final String RPAREN = "RPAREN"; // this is not consistent
                                                   // with the treebank trees
   public static final String POUND = "POUND"; // this is not consistent with
                                                // the treebank trees
   public static final String COMMA = ",";
   public static final String PERIOD = ".";


   public static List<String> getPOSSymbols() {
      List<String> r = new ArrayList<String>();
      r.add(CC);
      r.add(CD);
      r.add(DT);
      r.add(EX);
      r.add(FW);
      r.add(IN);
      r.add(JJ);
      r.add(JJR);
      r.add(JJS);
      r.add(LS);
      r.add(MD);
      r.add(NN);
      r.add(NNS);
      r.add(NNP);
      r.add(NNPS);
      r.add(PDT);
      r.add(POS);
      r.add(PRP);
      r.add(PRPDOLLAR);
      r.add(RB);
      r.add(RBR);
      r.add(RBS);
      r.add(RP);
      r.add(SYM);
      r.add(TO);
      r.add(UH);
      r.add(VB);
      r.add(VBD);
      r.add(VBG);
      r.add(VBN);
      r.add(VBP);
      r.add(VBZ);
      r.add(WDT);
      r.add(WP);
      r.add(WPDOLLAR);
      r.add(WRB);
      r.add(LQUOTE);
      r.add(RQUOTE);
      r.add(LPAREN);
      r.add(RPAREN);
      r.add(DOLLAR);
      r.add(POUND);
      r.add(COMMA);
      r.add(PERIOD);
      r.add(COLON);
      return r;
   }
}
