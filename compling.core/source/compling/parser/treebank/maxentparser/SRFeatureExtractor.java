
package compling.parser.treebank.maxentparser;

import compling.annotation.treebank.*;
import compling.grammar.cfg.*;
import compling.util.*;
import java.util.*;


class SRFeatureExtractor {

   static Interner interner;
   static final HeadedTreePrefs htp = new HeadedTreePrefs();


   static void init() {
      interner = new Interner();
   }


   private static void addInternedFeature(List l, String f) {
      if (MaxEntParserTrainer.secondPhase == false) {
         if (interner != null) {
            l.add(interner.intern(f));
         } else {
            l.add(f);
         }
      } else {
         if (MaxEntParserTrainer.featureCounter.containsKey(f) && interner != null) {
            l.add(interner.intern(f));
         }
      }
   }


   public static List extractSRFeatures(MaxEntDerivation med) {
      ArrayList features = new ArrayList();
      features.add(def());
      MaxEntTree met = med.getTreeFromOffset(-1);
      if (met.isComplete()) {
         features.add("completed");
      } else {
         int last = met.getChildren().size() - 1;
         int begin = 0;
         features.addAll(checkLast(med, last));
         features.addAll(checkBegin(med, begin));
         features.addAll(checkBeginLast(med, begin, last));
         for (int i = begin + 1; i < last; i++) {
            features.addAll(checkLastPair(med, last, i));
         }
         for (int i = last - 1; i > 0; i--) {
            features.addAll(checkBeginPair(med, begin, i));
         }
         addInternedFeature(features, met.getProduction());
         /*
          * HeadedTree possibleHead = htp.findHead(met.getLabel(),
          * met.getChildren()); addInternedFeature(features,
          * "PCT="+met.getLabel()+"|"+possibleHead.getHeadTag());
          * addInternedFeature(features,
          * "PCH="+met.getLabel()+"|"+possibleHead.getHead());
          * addInternedFeature(features,
          * "PC="+met.getLabel()+"|"+possibleHead.getLabel());
          */
         features.addAll(surround(med, 1));
         features.addAll(surround(med, 2));
         features.addAll(surround(med, -1));
         features.addAll(surround(med, -2));
         features.addAll(surround(med, -1, 1));
         features.addAll(surround(med, -2, -1));
         features.addAll(surround(med, 1, 2));
         features.addAll(stackTrace(med));
      }
      return features;
   }


   private static List checkLast(MaxEntDerivation med, int n) {
      ArrayList al = new ArrayList(3);
      MaxEntTree p = med.getTreeFromOffset(-1);

      HeadedTree c = p.getChild(n);
      String pl = p.getLabel();
      String cl = c.getLabel();
      String ct = c.getHeadTag();
      addInternedFeature(al, "cl**=" + pl + "|" + cl);
      addInternedFeature(al, "cl*=" + pl + "|" + cl + "|" + ct);
      addInternedFeature(al, "cl=" + pl + "|" + cl + "|" + ct + "|" + c.getHead());

      return al;
   }


   private static List checkBegin(MaxEntDerivation med, int n) {
      ArrayList al = new ArrayList(2);
      MaxEntTree p = med.getTreeFromOffset(-1);
      HeadedTree c = p.getChild(n);
      String pl = p.getLabel();
      String cl = c.getLabel();
      String ct = c.getHeadTag();
      addInternedFeature(al, "cb**=" + pl + "|" + cl);
      addInternedFeature(al, "cb*=" + pl + "|" + cl + "|" + ct);
      addInternedFeature(al, "cb=" + pl + "|" + cl + "|" + ct + "|" + c.getHead());
      return al;
   }


   private static List checkBeginLast(MaxEntDerivation med, int m, int n) {
      ArrayList al = new ArrayList(4);
      MaxEntTree p = med.getTreeFromOffset(-1);
      HeadedTree cm = p.getChild(m);
      HeadedTree cn = p.getChild(n);
      String pl = p.getLabel();
      String cml = cm.getLabel();
      String cnl = cn.getLabel();
      String cmh = cm.getHead();
      String cnh = cn.getHead();
      addInternedFeature(al, "ccb*l*=" + pl + "," + cml + "," + cnl);
      addInternedFeature(al, "ccbl*=" + pl + "," + cml + "|" + cmh + "," + cnl);
      addInternedFeature(al, "ccb*l=" + pl + "," + cml + "," + cnl + "|" + cnh);
      addInternedFeature(al, "ccbl=" + pl + "," + cml + "|" + cmh + "," + cnl + "|"
            + cnh);
      return al;
   }


   private static List checkBeginPair(MaxEntDerivation med, int m, int n) {
      ArrayList al = new ArrayList(4);
      MaxEntTree p = med.getTreeFromOffset(-1);
      MaxEntTree cm = p.getChild(m);
      MaxEntTree cn = p.getChild(n);
      String pl = p.getLabel();
      String cml = cm.getLabel();
      String cnl = cn.getLabel();
      String cmh = cm.getHead();
      String cnh = cn.getHead();
      addInternedFeature(al, "ccb*o*=" + pl + "," + cml + "," + cnl);
      addInternedFeature(al, "ccbo*=" + pl + "," + cml + "|" + cmh + "," + cnl);
      addInternedFeature(al, "ccb*o=" + pl + "," + cml + "," + cnl + "|" + cnh);
      addInternedFeature(al, "ccbo=" + pl + "," + cml + "|" + cmh + "," + cnl + "|"
            + cnh);
      return al;
   }


   private static List checkLastPair(MaxEntDerivation med, int m, int n) {
      ArrayList al = new ArrayList(4);
      MaxEntTree p = med.getTreeFromOffset(-1);
      MaxEntTree cm = p.getChild(m);
      MaxEntTree cn = p.getChild(n);
      String pl = p.getLabel();
      String cml = cm.getLabel();
      String cnl = cn.getLabel();
      String cmh = cm.getHead();
      String cnh = cn.getHead();
      addInternedFeature(al, "ccl*o*=" + pl + "," + cml + "," + cnl);
      addInternedFeature(al, "cclo*=" + pl + "," + cml + "|" + cmh + "," + cnl);
      addInternedFeature(al, "ccl*o=" + pl + "," + cml + "," + cnl + "|" + cnh);
      addInternedFeature(al, "cclo=" + pl + "," + cml + "|" + cmh + "," + cnl + "|"
            + cnh);
      return al;
   }


   private static List surround(MaxEntDerivation med, int n) {
      ArrayList al = new ArrayList(2);
      MaxEntTree met = med.getTreeFromOffset(-1);
      String tag = met.getEdgePOS(n);
      String word = met.getEdgeWord(n);
      ;
      addInternedFeature(al, "s" + n + "=" + tag + "|" + word);
      addInternedFeature(al, "s*" + n + "=" + tag);
      return al;
   }


   private static List surround(MaxEntDerivation med, int l, int r) {
      ArrayList al = new ArrayList(4);
      List right = surround(med, r);
      List left = surround(med, l);
      if (left.size() < 1 || right.size() < 1) {
         return al;
      }
      addInternedFeature(al, left.get(0) + "," + right.get(0));
      if (right.size() > 1)
         addInternedFeature(al, left.get(0) + "," + right.get(1));
      if (left.size() > 1)
         addInternedFeature(al, left.get(1) + "," + right.get(0));
      if (right.size() > 1 && left.size() > 1)
         addInternedFeature(al, left.get(1) + "," + right.get(1));
      return al;
   }


   private static List stackTrace(MaxEntDerivation med) {
      ArrayList al = new ArrayList(7);
      MaxEntTree zero = med.getTreeFromOffset(-1);
      MaxEntTree minusOne = med.getTreeFromOffset(-2);
      MaxEntTree minusTwo = med.getTreeFromOffset(-3);
      String zeroLabel = zero.getLabel();
      String minusOneLabel = "NONE";
      if (minusOne != null) {
         minusOneLabel = minusOne.getLabel();
      }
      String minusTwoLabel = "NONE";
      if (minusTwo != null) {
         minusOneLabel = minusTwo.getLabel();
      }
      String tag = zero.getEdgePOS(1);
      String twoTag = zero.getEdgePOS(2);
      String word = zero.getEdgeWord(1);
      String riw = zero.rightInternalWord;
      String rit = zero.rightInternalPOS;
      addInternedFeature(al, "st0=" + zeroLabel + "|" + tag + "|" + word);
      addInternedFeature(al, "st0*=" + zeroLabel + "|" + tag);
      addInternedFeature(al, "st1=" + minusOneLabel + "|" + zeroLabel + "|" + tag
            + "|" + word);
      addInternedFeature(al, "st1*=" + minusOneLabel + "|" + zeroLabel + "|" + tag);
      addInternedFeature(al, "st2=" + minusTwoLabel + "|" + minusOneLabel + "|"
            + zeroLabel + "|" + tag + "|" + word);
      addInternedFeature(al, "st2*=" + minusTwoLabel + "|" + minusOneLabel + "|"
            + zeroLabel + "|" + tag);
      addInternedFeature(al, "stt=" + zeroLabel + "|" + tag + "|" + twoTag);
      addInternedFeature(al, "stt1*=" + minusOneLabel + "|" + zeroLabel + "|" + tag
            + "|" + twoTag);
      addInternedFeature(al, "sti0=" + zeroLabel + "|" + tag + "|" + word + "|" + rit
            + "|" + riw);
      addInternedFeature(al, "sti0*=" + zeroLabel + "|" + tag + "|" + rit + "|" + riw);
      addInternedFeature(al, "sti*0=" + zeroLabel + "|" + tag + "|" + word + "|"
            + rit);
      addInternedFeature(al, "sti*0*=" + zeroLabel + "|" + tag + "|" + rit);
      // addInternedFeature(al,
      // "sti1="+minusOneLabel+"|"+zeroLabel+"|"+tag+"|"+word+"|"+rit+"|"+riw);
      // addInternedFeature(al,
      // "sti1*="+minusOneLabel+"|"+zeroLabel+"|"+tag+"|"+rit+"|"+riw);
      // addInternedFeature(al,
      // "sti*1="+minusOneLabel+"|"+zeroLabel+"|"+tag+"|"+word+"|"+rit);
      addInternedFeature(al, "sti*1*=" + minusOneLabel + "|" + zeroLabel + "|" + tag
            + "|" + rit);

      return al;
   }


   private static String def() {
      return "DEFAULT";
   }

}

/*
 * 
 * private static List checkcons(MaxEntDerivation med, int n){ ArrayList al =
 * new ArrayList(2); MaxEntTree p = med.getTreeFromOffset(0); MaxEntTree c
 * =p.getChild(n); String pl = p.getLabel(); String cl = c.getLabel();
 * addInternedFeature(al,"cc*" + n + "=" + pl +"|" + cl); addInternedFeature(al,
 * "cc" + n + "=" + pl + "|" + cl + "|" + c.getHead()); return al; }
 * 
 */
