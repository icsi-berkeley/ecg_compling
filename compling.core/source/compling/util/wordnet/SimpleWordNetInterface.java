
package compling.util.wordnet;

import java.io.FileInputStream;
import java.util.Dictionary;
import java.util.List;


/**
 * This class is intended to provide a simple WordNet interface without have to
 * worry about WordSenses and all the complexities of actual using the wordnet
 * package.
 * 
 * All of these functions only look at sense 1.
 * 
 * @author John Bryant
 * 
 */

public class SimpleWordNetInterface {

   static IndexWord ANIMATETHINGIW;
   static IndexWord PHYSOBJIW;
   static IndexWord SUBSTANCEIW;
   static DefaultMorphologicalProcessor dmp = new DefaultMorphologicalProcessor();

   static {
      String propsFile = "file_properties.xml";
      try {
         JWNL.initialize(new FileInputStream(propsFile));
         ANIMATETHINGIW = Dictionary.getInstance().getIndexWord(POS.NOUN,
               "living_thing");
         PHYSOBJIW = Dictionary.getInstance().getIndexWord(POS.NOUN, "object");
         SUBSTANCEIW = Dictionary.getInstance().getIndexWord(POS.NOUN, "substance");
      } catch (Exception ex) {
         System.out.println("Couldn't load wordnet " + ex);
         // FIXME: System.exit()!!
         System.exit(-1);
      }
   }


   public static boolean isAnimate(String w) {
      try {
         IndexWord iw = Dictionary.getInstance().getIndexWord(POS.NOUN, w);
         if (iw == null) {
            return false;
         }
         PointerTargetTree hypernyms = PointerUtils.getInstance().getHypernymTree(
               iw.getSense(1));

         if (hypernyms.findFirst(ANIMATETHINGIW.getSense(1)) != null) {
            return true;
         }
      } catch (JWNLException j) {
         System.out.println("Word: " + w + "caused this: " + j);
      }
      return false;
   }


   public static boolean isPhysicalObject(String w) {
      try {
         IndexWord iw = Dictionary.getInstance().getIndexWord(POS.NOUN, w);
         if (iw == null) {
            return false;
         }
         PointerTargetTree hypernyms = PointerUtils.getInstance().getHypernymTree(
               iw.getSense(1));

         if (hypernyms.findFirst(PHYSOBJIW.getSense(1)) != null) {
            return true;
         }
      } catch (JWNLException j) {
         System.out.println("Word: " + w + "caused this: " + j);
      }
      return false;
   }


   public static boolean isSubstance(String w) {
      try {
         IndexWord iw = Dictionary.getInstance().getIndexWord(POS.NOUN, w);
         if (iw == null) {
            return false;
         }
         PointerTargetTree hypernyms = PointerUtils.getInstance().getHypernymTree(
               iw.getSense(1));

         if (hypernyms.findFirst(SUBSTANCEIW.getSense(1)) != null) {
            return true;
         }
      } catch (JWNLException j) {
         System.out.println("Word: " + w + "caused this: " + j);
      }
      return false;
   }


   public static boolean inWordNet(String w) {
      try {
         IndexWord iw = Dictionary.getInstance().getIndexWord(POS.NOUN, w);
         if (iw == null) {
            return false;
         }
         return true;
      } catch (JWNLException j) {
         System.out.println("Word: " + w + "caused this: " + j);
      }
      return false;
   }


   public static boolean nounSubType(String child, String parent) {
      try {
         IndexWord ic = Dictionary.getInstance().getIndexWord(POS.NOUN, child);
         if (ic == null) {
            return false;
         }
         PointerTargetTree hypernyms = PointerUtils.getInstance().getHypernymTree(
               ic.getSense(1));

         IndexWord ip = Dictionary.getInstance().getIndexWord(POS.NOUN, parent);
         if (ip == null) {
            return false;
         }

         if (hypernyms.findFirst(ip.getSense(1)) != null) {
            return true;
         }
      } catch (JWNLException j) {
         System.out.println("Words: " + child + " and " + "caused this: " + j);
      }
      return false;
   }


   public static String getNounAncestorAtDistance(String w, int distance) {
      try {
         IndexWord iw = Dictionary.getInstance().getIndexWord(POS.NOUN, w);
         if (iw == null) {
            return null;
         }
         PointerTargetTree hypernyms = PointerUtils.getInstance().getHypernymTree(
               iw.getSense(1));
         List l = hypernyms.toList();
         PointerTargetNodeList ptl = (PointerTargetNodeList) l.get(0);
         /*
          * for (int i = 0; i < ptl.size(); i++){ PointerTargetNode pt =
          * (PointerTargetNode) ptl.get(i);
          * System.out.print(pt.getSynset().getWord(0).getLemma()+"; "); }
          */
         if (distance < 0) {
            distance = ptl.size() + distance;
            if (distance < 0)
               distance = 0;
         } else if (distance >= ptl.size()) {
            distance = ptl.size() - 1;
         }
         return ((PointerTargetNode) ptl.get(distance)).getSynset().getWord(0)
               .getLemma();
      } catch (JWNLException j) {
         System.out.println("Word: " + w + "caused this: " + j);
      }
      return null;
   }


   public static void main(String[] args) {
      System.out
            .println("is dog animate? " + SimpleWordNetInterface.isAnimate("dog"));
      System.out.println("is rock animate? "
            + SimpleWordNetInterface.isAnimate("rock"));
      System.out.println("is dog a physical object? "
            + SimpleWordNetInterface.isPhysicalObject("dog"));
      System.out.println("is rock a physical object? "
            + SimpleWordNetInterface.isPhysicalObject("rock"));
      System.out.println("is money a physical object? "
            + SimpleWordNetInterface.isPhysicalObject("money"));
      System.out.println("is dog a substance? "
            + SimpleWordNetInterface.isSubstance("dog"));
      System.out.println("is money a substance? "
            + SimpleWordNetInterface.isSubstance("money"));
      System.out.println("is butter a substance? "
            + SimpleWordNetInterface.isSubstance("butter"));
      System.out.println("is money a subtype of dog? "
            + SimpleWordNetInterface.nounSubType("money", "dog"));
      System.out.println("is money a subtype of substance? "
            + SimpleWordNetInterface.nounSubType("money", "substance"));
      System.out.println("is living_thing a subtype of object? "
            + SimpleWordNetInterface.nounSubType("living_thing", "object"));
      System.out.println("is object a subtype of object? "
            + SimpleWordNetInterface.nounSubType("object", "object"));
      System.out.println("problems " + SimpleWordNetInterface.inWordNet("problem"));
      System.out.println("federal_government "
            + SimpleWordNetInterface.inWordNet("federal_government"));
      System.out.println(getNounAncestorAtDistance("dog", 0));
      System.out.println(getNounAncestorAtDistance("dog", 1));
      System.out.println(getNounAncestorAtDistance("dog", 2));
      System.out.println(getNounAncestorAtDistance("dog", -1));
      System.out.println(getNounAncestorAtDistance("dog", -2));
      System.out.println(getNounAncestorAtDistance("dog", -3));
      System.out.println(getNounAncestorAtDistance("idea", -2));

   }

}
