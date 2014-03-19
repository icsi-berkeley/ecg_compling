
package compling.parser.treebank.maxentparser;

//import compling.annotation.treebank.*;
//import compling.syntax.*;
import compling.util.*;
import java.util.*;


class BCFeatureExtractor {

   static Interner interner;
   static final String NONE = "*N*";
   static final String BEGIN = "B";
   static final String CONT = "C";


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


   public static List extractBCFeatures(MaxEntDerivation med) {
      ArrayList features = new ArrayList();
      features.add(def());
      MaxEntTree zero = med.getTreeFromOffset(-1);
      MaxEntTree one = med.getTreeFromOffset(0);
      MaxEntTree two = med.getTreeFromOffset(1);
      MaxEntTree three = med.getTreeFromOffset(2);
      MaxEntTree four = med.getTreeFromOffset(3);
      MaxEntTree five = med.getTreeFromOffset(4);
      MaxEntTree minusOne = med.getTreeFromOffset(-2);
      MaxEntTree minusTwo = med.getTreeFromOffset(-3);
      // MaxEntTree minusThree = med.getTreeFromOffset(-4);
      String[] bcAnnos = new String[8];
      String[] headTags = new String[8];
      String[] parents = new String[8];
      String[] labels = new String[8];
      String[] heads = new String[8];
      if (med.chunkPhase && med.lastOp.equals("R")) {
         features.add("MUSTNOOP");
      }
      if (med.chunkPhase && minusOne != null && minusOne.isComplete() == false) {
         features.add("CANTNOOP");
      }

      setUpFeatures(2, -1, zero, parents, bcAnnos, labels, heads, headTags);
      setUpFeatures(3, -1, one, parents, bcAnnos, labels, heads, headTags);
      setUpFeatures(4, -1, two, parents, bcAnnos, labels, heads, headTags);
      setUpFeatures(5, -1, three, parents, bcAnnos, labels, heads, headTags);
      setUpFeatures(6, -1, four, parents, bcAnnos, labels, heads, headTags);
      setUpFeatures(7, -1, five, parents, bcAnnos, labels, heads, headTags);
      setUpFeatures(1, 1, minusOne, parents, bcAnnos, labels, heads, headTags);
      if (minusOne != null && minusOne.getChildren().size() > 1) {
         setUpFeatures(0, 2, minusOne, parents, bcAnnos, labels, heads, headTags);
      } else {
         setUpFeatures(0, 1, minusTwo, parents, bcAnnos, labels, heads, headTags);
      }

      features.addAll(cons(bcAnnos, parents, labels, heads, headTags));
      features.addAll(consPairs(bcAnnos, parents, labels, heads, headTags));
      features.addAll(consTriples(bcAnnos, parents, labels, heads, headTags));
      if (zero.isComma && minusOne != null && minusOne.hasComma) {
         features.add("joinComma");
      }
      if (zero.isRightBracket && minusOne != null && minusOne.hasLeftBracket) {
         features.add("joinBracket");
      }
      if (zero.isPeriod && minusOne != null && minusOne.startsAtZero) {
         features.add("joinPeriod");
      }
      return features;
   }


   private static void setUpFeatures(int i, int offset, MaxEntTree met,
         String[] parents, String[] bcAnnos, String[] labels, String[] heads,
         String[] headTags) {
      if (met == null) {
         labels[i] = heads[i] = parents[i] = bcAnnos[i] = headTags[i] = NONE;
      } else if (met.isComplete()) {
         parents[i] = NONE;
         bcAnnos[i] = NONE;
         labels[i] = met.getLabel();
         heads[i] = met.getHead();
         headTags[i] = met.getHeadTag();
      } else {
         int childnum = met.getChildren().size() - offset;
         MaxEntTree child = ((MaxEntTree) met.getChildren().get(childnum));
         labels[i] = child.getLabel();
         heads[i] = child.getHead();
         headTags[i] = child.getHeadTag();
         parents[i] = met.getLabel();
         if (childnum > 0) {
            bcAnnos[i] = CONT;
         } else if (childnum == 0) {
            bcAnnos[i] = BEGIN;
         }
      }
      headTags[i] = ""; // I'm doing this because adding these in didn't seem to
                        // help any.
   }


   /** Generates the features for cons values -2, -1, 0, 1, 2 */
   private static List cons(String[] bcAnnos, String[] parents, String[] labels,
         String[] heads, String[] headTags) {
      ArrayList al = new ArrayList();
      addInternedFeature(al, "m2*=" + bcAnnos[0] + parents[0] + "|" + labels[0]);
      addInternedFeature(al, "m2=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + heads[0]);
      // addInternedFeature(al, "m2t="+
      // bcAnnos[0]+parents[0]+"|"+labels[0]+"|"+headTags[0]);

      addInternedFeature(al, "m1*=" + bcAnnos[1] + parents[1] + "|" + labels[1]);
      addInternedFeature(al, "m1=" + bcAnnos[1] + parents[1] + "|" + labels[1] + "|"
            + heads[1]);
      // addInternedFeature(al, "m1t="+
      // bcAnnos[1]+parents[1]+"|"+labels[1]+"|"+headTags[1]);

      addInternedFeature(al, "0*=" + labels[2]);
      addInternedFeature(al, "0=" + labels[2] + "|" + heads[2]);
      // addInternedFeature(al, "0t="+ labels[2]+"|"+headTags[2]);

      addInternedFeature(al, "1*=" + labels[3]);
      addInternedFeature(al, "1=" + labels[3] + "|" + heads[3]);

      addInternedFeature(al, "2*=" + labels[4]);
      addInternedFeature(al, "2=" + labels[4] + "|" + heads[4]);

      /* extra features */

      addInternedFeature(al, "3*=" + labels[5]);
      addInternedFeature(al, "3=" + labels[5] + "|" + heads[5]);

      addInternedFeature(al, "4*=" + labels[6]);
      addInternedFeature(al, "4=" + labels[6] + "|" + heads[6]);

      addInternedFeature(al, "5*=" + labels[7]);
      addInternedFeature(al, "5=" + labels[7] + "|" + heads[7]);

      return al;
   }


   /** Generates the features for cons values (-1,0), (0,1) */
   private static List consPairs(String[] bcAnnos, String[] parents, String[] labels,
         String[] heads, String[] headTags) {
      ArrayList al = new ArrayList();

      addInternedFeature(al, "1*0*=" + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "," + labels[2] + "|" + headTags[2]);
      addInternedFeature(al, "10*=" + bcAnnos[1] + parents[1] + "|" + labels[1] + "|"
            + headTags[1] + "|" + heads[1] + "," + labels[2] + "|" + headTags[2]);
      addInternedFeature(al, "1*0=" + bcAnnos[1] + parents[1] + "|" + labels[1] + "|"
            + headTags[1] + "," + labels[2] + "|" + headTags[2] + "|" + heads[2]);
      addInternedFeature(al, "10=" + bcAnnos[1] + parents[1] + "|" + labels[1] + "|"
            + headTags[1] + "|" + heads[1] + "," + labels[2] + "|" + headTags[2]
            + "|" + heads[2]);

      addInternedFeature(al, "0*1*=" + labels[2] + "|" + headTags[2] + ","
            + labels[3]);
      addInternedFeature(al, "0*1=" + labels[2] + "|" + headTags[2] + "," + labels[3]
            + "|" + heads[3]);
      addInternedFeature(al, "01*=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[3]);
      addInternedFeature(al, "01=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[3] + "|" + heads[3]);

      /* extra features */

      addInternedFeature(al, "2*0*=" + bcAnnos[0] + parents[0] + "|" + labels[0]
            + "|" + headTags[0] + "," + labels[2] + "|" + headTags[2]);
      addInternedFeature(al, "20*=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + headTags[0] + "|" + heads[0] + "," + labels[2] + "|" + headTags[2]);
      addInternedFeature(al, "2*0=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + headTags[0] + "," + labels[2] + "|" + headTags[2] + "|" + heads[2]);
      addInternedFeature(al, "20=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + headTags[0] + "|" + heads[0] + "," + labels[2] + "|" + headTags[2]
            + "|" + heads[2]);

      addInternedFeature(al, "2*1*=" + bcAnnos[0] + parents[0] + "|" + labels[0]
            + "|" + headTags[0] + "," + bcAnnos[1] + labels[1] + "|" + headTags[1]);
      addInternedFeature(al, "21*=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + headTags[0] + "|" + heads[0] + "," + bcAnnos[1] + labels[1] + "|"
            + headTags[1]);
      addInternedFeature(al, "2*1=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + headTags[0] + "," + bcAnnos[1] + labels[1] + "|" + headTags[1] + "|"
            + heads[1]);
      addInternedFeature(al, "21=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + headTags[0] + "|" + heads[0] + "," + bcAnnos[1] + labels[1] + "|"
            + headTags[1] + "|" + heads[1]);

      addInternedFeature(al, "1*2*=" + labels[3] + "," + labels[4]);
      addInternedFeature(al, "1*2=" + labels[3] + "," + labels[4] + "|" + heads[4]);
      addInternedFeature(al, "12*=" + labels[3] + "|" + heads[3] + "," + labels[4]);
      addInternedFeature(al, "12=" + labels[3] + "|" + heads[3] + "," + labels[4]
            + "|" + heads[4]);

      addInternedFeature(al, "2*3*=" + labels[4] + "," + labels[5]);
      addInternedFeature(al, "2*3=" + labels[4] + "," + labels[5] + "|" + heads[5]);
      addInternedFeature(al, "23*=" + labels[4] + "|" + heads[4] + "," + labels[5]);
      addInternedFeature(al, "23=" + labels[4] + "|" + heads[4] + "," + labels[5]
            + "|" + heads[5]);

      addInternedFeature(al, "0*2*=" + labels[2] + "|" + headTags[2] + ","
            + labels[4]);
      addInternedFeature(al, "0*2=" + labels[2] + "|" + headTags[2] + "," + labels[4]
            + "|" + heads[4]);
      addInternedFeature(al, "02*=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[4]);
      addInternedFeature(al, "02=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[4] + "|" + heads[4]);

      addInternedFeature(al, "0*3*=" + labels[2] + "|" + headTags[2] + ","
            + labels[5]);
      addInternedFeature(al, "0*3=" + labels[2] + "|" + headTags[2] + "," + labels[5]
            + "|" + heads[5]);
      addInternedFeature(al, "03*=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[5]);
      addInternedFeature(al, "03=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[5] + "|" + heads[5]);

      addInternedFeature(al, "0*4*=" + labels[2] + "|" + headTags[2] + ","
            + labels[6]);
      addInternedFeature(al, "0*4=" + labels[2] + "|" + headTags[2] + "," + labels[6]
            + "|" + heads[6]);
      addInternedFeature(al, "04*=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[6]);
      addInternedFeature(al, "04=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[6] + "|" + heads[6]);

      addInternedFeature(al, "0*5*=" + labels[2] + "|" + headTags[2] + ","
            + labels[7]);
      addInternedFeature(al, "0*5=" + labels[2] + "|" + headTags[2] + "," + labels[7]
            + "|" + heads[7]);
      addInternedFeature(al, "05*=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[7]);
      addInternedFeature(al, "05=" + labels[2] + "|" + headTags[2] + "|" + heads[2]
            + "," + labels[7] + "|" + heads[7]);

      return al;
   }


   /** Generates the features for cons values (-2, -1, 0), (-1, 0, 1), (0, 1, 2) */
   private static List consTriples(String[] bcAnnos, String[] parents,
         String[] labels, String[] heads, String[] headTags) {
      ArrayList al = new ArrayList();

      addInternedFeature(al, "210t=" + bcAnnos[0] + parents[0] + "|" + labels[0]
            + "|" + headTags[0] + "," + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "," + labels[2] + "|" + headTags[2]);
      addInternedFeature(al, "2*1*0=" + bcAnnos[0] + parents[0] + "|" + labels[0]
            + "|" + headTags[0] + "," + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "," + labels[2] + "|" + headTags[2] + "|"
            + heads[2]);
      addInternedFeature(al, "2*10=" + bcAnnos[0] + parents[0] + "|" + labels[0]
            + "|" + headTags[0] + "," + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "|" + heads[1] + "," + labels[2] + "|"
            + headTags[2] + "|" + heads[2]);
      addInternedFeature(al, "21*0=" + bcAnnos[0] + parents[0] + "|" + labels[0]
            + "|" + headTags[0] + "|" + heads[0] + "," + bcAnnos[1] + parents[1]
            + "|" + labels[1] + "|" + headTags[1] + "," + labels[2] + "|"
            + headTags[2] + "|" + heads[2]);
      addInternedFeature(al, "210=" + bcAnnos[0] + parents[0] + "|" + labels[0] + "|"
            + headTags[0] + "|" + heads[0] + "," + bcAnnos[1] + parents[1] + "|"
            + labels[1] + "|" + headTags[1] + "|" + heads[1] + "," + labels[2] + "|"
            + headTags[2] + "|" + heads[2]);

      addInternedFeature(al, "101t=" + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "," + labels[2] + "|" + headTags[2] + ","
            + labels[3]);
      addInternedFeature(al, "1*01*=" + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "," + labels[2] + "|" + headTags[2] + "|"
            + heads[2] + "," + labels[3]);
      addInternedFeature(al, "101*=" + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "|" + heads[1] + "," + labels[2] + "|"
            + headTags[2] + "|" + heads[2] + "," + labels[3]);
      addInternedFeature(al, "1*01=" + bcAnnos[1] + parents[1] + "|" + labels[1]
            + "|" + headTags[1] + "," + labels[2] + "|" + headTags[2] + "|"
            + heads[2] + "," + labels[3] + "|" + heads[3]);
      addInternedFeature(al, "101=" + bcAnnos[1] + parents[1] + "|" + labels[1] + "|"
            + headTags[1] + "|" + heads[1] + "," + labels[2] + "|" + headTags[2]
            + "|" + heads[2] + "," + labels[3] + "|" + heads[3]);

      addInternedFeature(al, "01*2*t=" + labels[2] + "|" + headTags[2] + ","
            + labels[3] + "," + labels[4]);
      addInternedFeature(al, "01*2*=" + labels[2] + "|" + heads[2] + "," + labels[3]
            + "," + labels[4]);
      addInternedFeature(al, "012*=" + labels[2] + "|" + heads[2] + "," + labels[3]
            + "|" + heads[3] + "," + labels[4]);
      addInternedFeature(al, "01*2=" + labels[2] + "|" + heads[2] + "," + labels[3]
            + "," + labels[4] + "|" + heads[4]);
      addInternedFeature(al, "012=" + labels[2] + "|" + heads[2] + "," + labels[3]
            + "|" + heads[3] + "," + labels[4] + "|" + heads[4]);

      // addInternedFeature(al,
      // "0123t="+labels[2]+"|"+headTags[2]+","+labels[3]+","+labels[4]+","+labels[5]);

      return al;
   }


   private static String def() {
      return "DEFAULT";
   }
}
