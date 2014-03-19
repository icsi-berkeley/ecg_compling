
package compling.parser.treebank.maxentparser;

import compling.annotation.treebank.*;
import compling.grammar.cfg.*;

import java.util.*;
import compling.utterance.*;


public class MaxEntTree extends HeadedTree {

   static final String UNKNOWNHEAD = "UNKNOWNHEAD";
   static final HeadedTreePrefs htp = new HeadedTreePrefs();

   boolean completedSubTree = false;
   boolean hasLeftBracket = false;
   boolean startsAtZero = false;
   boolean hasComma = false;
   boolean isComma = false;
   boolean isPeriod = false;
   boolean isRightBracket = false;
   String[] rightPOSs;
   String[] rightWords;
   String[] leftPOSs;
   String[] leftWords;
   StringBuffer prod;
   String rightInternalWord = "";
   String rightInternalPOS = "";


   public MaxEntTree(String sym, List child) {
      super(sym, UNKNOWNHEAD, UNKNOWNHEAD, child);
      MaxEntTree met = (MaxEntTree) child.get(0);
      leftPOSs = met.leftPOSs;
      leftWords = met.leftWords;
      startsAtZero = met.startsAtZero;
      prod = new StringBuffer(label).append("->");
      newChildStuff(met);
      isRightBracket = false;
      isComma = false;
      isPeriod = false;
   }


   public MaxEntTree(Sentence s, int position, boolean LR) {
      // public MaxEntTree(Sentence s, int position){
      super(s.getWord(position).getPOSTag(), s.getWord(position).getOrth());
      rightInternalWord = s.getWord(position).getOrth();
      rightInternalPOS = s.getWord(position).getPOSTag();
      completedSubTree = true;
      if (position == 0) {
         startsAtZero = true;
      }
      if (LR) {
         hasLeftBracket = s.getWord(position).getOrth().equals("-LRB-");
      } else {
         hasLeftBracket = s.getWord(position).getOrth().equals("-RRB-");
      }
      hasComma = isComma = s.getWord(position).getOrth().equals(",");
      if (LR) {
         isRightBracket = s.getWord(position).getOrth().equals("-RRB-");
      } else {
         isRightBracket = s.getWord(position).getOrth().equals("-LRB-");
      }
      isPeriod = s.getWord(position).getOrth().equals(".");
      rightPOSs = new String[2];
      rightWords = new String[2];
      leftPOSs = new String[2];
      leftWords = new String[2];
      rightPOSs[0] = s.getWord(position + 1).getPOSTag();
      rightPOSs[1] = s.getWord(position + 2).getPOSTag();
      leftPOSs[0] = s.getWord(position - 1).getPOSTag();
      leftPOSs[1] = s.getWord(position - 2).getPOSTag();
      rightWords[0] = s.getWord(position + 1).getOrth();
      rightWords[1] = s.getWord(position + 2).getOrth();
      leftWords[0] = s.getWord(position - 1).getOrth();
      leftWords[1] = s.getWord(position - 2).getOrth();
      prod = new StringBuffer(label).append("->").append(
            s.getWord(position).getOrth());
   }


   public MaxEntTree shallowCopy() {
      return new MaxEntTree(this);
   }


   public boolean isComplete() {
      return completedSubTree;
   }


   private MaxEntTree(MaxEntTree m) {
      super(m.getLabel(), m.getHead(), m.getHeadTag(), (List) ((ArrayList) m
            .getChildren()).clone());
      leftPOSs = m.leftPOSs;
      leftWords = m.leftWords;
      startsAtZero = m.startsAtZero;
      prod = new StringBuffer(m.prod.toString());
      rightPOSs = m.rightPOSs;
      rightWords = m.rightWords;
      hasLeftBracket = m.hasLeftBracket;
      hasComma = m.hasComma;
      isRightBracket = false;
      isComma = false;
      isPeriod = false;
      completedSubTree = m.isComplete();
   }


   public MaxEntTree getChild(int i) {
      return (MaxEntTree) children.get(i);
   }


   public void addChild(MaxEntTree met) {
      children.add(met);
      newChildStuff(met);
   }


   public void finish() {
      completedSubTree = true;
      HeadedTree ht = htp.findHead(label, children);
      setHead(ht.getHead());
      setHeadTag(ht.getHeadTag());
   }


   public String getProduction() {
      return prod.toString();
   }


   private void newChildStuff(MaxEntTree met) {
      rightPOSs = met.rightPOSs;
      rightWords = met.rightWords;
      hasLeftBracket = hasLeftBracket || met.hasLeftBracket;
      prod.append(met.getLabel()).append("|");
      hasComma = hasComma || met.hasComma;
      rightInternalWord = met.rightInternalWord;
      rightInternalPOS = met.rightInternalPOS;
   }


   public String getEdgePOS(int i) {
      switch (i) {
         case 1:
            return rightPOSs[0];
         case 2:
            return rightPOSs[1];
         case -1:
            return leftPOSs[0];
         case -2:
            return leftPOSs[1];
         default:
            System.out.println("error in getEdgePOS");
      }
      return "";
   }


   public String getEdgeWord(int i) {
      switch (i) {
         case 1:
            return rightWords[0];
         case 2:
            return rightWords[1];
         case -1:
            return leftWords[0];
         case -2:
            return leftWords[1];
         default:
            System.out.println("error in getEdgeWord");
      }
      return "";
   }

}
