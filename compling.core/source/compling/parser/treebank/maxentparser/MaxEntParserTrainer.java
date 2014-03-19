
package compling.parser.treebank.maxentparser;

import compling.util.*;
import compling.util.fileutil.*;
import compling.annotation.treebank.*;
import compling.annotation.treebank.TreeUtilities.*;
import compling.classifier.*;
import compling.classifier.MaximumEntropyClassifierTester.*;
import compling.grammar.cfg.*;
import compling.utterance.*;
import java.util.*;
import java.io.*;


class MaxEntParserTrainer {

   static Counter featureCounter = new Counter();
   public static final String SHIFT = "S";
   public static final String REDUCE = "R";
   public static final String CONT = "C";
   public static final String BEGIN = "B";
   public static final String NOOP = "NOOP";

   static Interner interner = new Interner();
   static final int NUMITERATIONS = 60;
   static List extractedData = new ArrayList();
   static boolean secondPhase = false;
   static boolean twoPhaseParsing = true;
   public static boolean chunkPhase = false;


   private static void addInternedFeature(List l, String f) {
      l.add(f);
   }


   public static void trainClassifier(boolean SR, int startSection, int endSection,
         String path, boolean leftToRight, int cutoff, int maxLength) {
      System.out.println("Extracting features: SR=" + SR + "  leftToRight="
            + leftToRight + "  maxSentenceLength=" + maxLength + "  featcutoff="
            + cutoff + "  endSection=" + endSection + "  chunkPhase=" + chunkPhase);
      interner = new Interner();
      StandardTreeNormalizer trans = new StandardTreeNormalizer();
      FileFilter ff = new NumberRangeFileFilter(".mrg", startSection, endSection,
            true);

      int i = 0;
      for (TreeBankFileIterator tfi = new TreeBankFileIterator(path, ff, trans); tfi
            .hasNext();) {
         Sentence s = tfi.nextSentence();
         if (s.getLength() <= maxLength) {
            addFeaturesToCounter(getTrainingData(s, SR, leftToRight, chunkPhase));
            i++;
            if (i % 5000 == 0) {
               System.out.println("Tree " + i + " analyzed");
            }
         }
      }
      System.out.println("Went through the trees");
      // unigram = null;
      featureCounter.removeLowCountKeys(cutoff);
      System.out.println("Removed low keys");
      SRFeatureExtractor.init();
      BCFeatureExtractor.init();
      // removeLowCountFeatures(extractedData);

      i = 0;
      secondPhase = true;
      ArrayList allTrainingData = new ArrayList();
      for (TreeBankFileIterator tfi = new TreeBankFileIterator(path, ff, trans); tfi
            .hasNext();) {
         Sentence s = tfi.nextSentence();
         if (s.getLength() <= maxLength) {
            allTrainingData.addAll(removeLowCountFeatures(getTrainingData(s, SR,
                  leftToRight, chunkPhase)));
            i++;
            if (i % 5000 == 0) {
               System.out.println("Tree " + i + " analyzed");
            }
         }
      }

      featureCounter = new Counter();
      SRFeatureExtractor.init();
      BCFeatureExtractor.init();
      buildAndSaveClassifier(allTrainingData, SR, leftToRight, maxLength, cutoff,
            endSection);
   }


   private static void buildAndSaveClassifier(List trainingData, boolean SR,
         boolean leftToRight, int maxLength, int cutoff, int endSection) {
      MaximumEntropyClassifier.Factory maximumEntropyClassifierFactory = new MaximumEntropyClassifier.Factory(
            1.0, NUMITERATIONS);
      System.out.println("Beginning training: SR=" + SR + "  leftToRight="
            + leftToRight + "  maxSentenceLength=" + maxLength + "  featcutoff="
            + cutoff);
      ProbabilisticClassifier classifier = (ProbabilisticClassifier) maximumEntropyClassifierFactory
            .trainClassifier(trainingData);

      try {
         System.out.println("Saving classifier: SR=" + SR + "  leftToRight="
               + leftToRight + "  maxSentenceLength=" + maxLength + "  featcutoff="
               + cutoff);
         // Serialize to a file
         ObjectOutput out = new ObjectOutputStream(new FileOutputStream(makeFileName(
               SR, leftToRight, maxLength, cutoff, endSection, chunkPhase)));
         out.writeObject(classifier);
         out.close();
         System.out.println("done");
      } catch (IOException e) {
         System.out.println("Could not write model file" + e);
      }
   }


   static void addFeaturesToCounter(List labeledData) {
      for (Iterator i = labeledData.iterator(); i.hasNext();) {
         BasicLabeledDatum bld = (BasicLabeledDatum) i.next();
         Collection featureList = bld.getFeatures();
         for (Iterator j = featureList.iterator(); j.hasNext();) {
            String feature = (String) j.next();
            featureCounter.incrementCount(feature, 1);
         }
         // extractedData.add(bld);
      }
   }


   static List removeLowCountFeatures(List labeledData) {
      for (Iterator i = labeledData.iterator(); i.hasNext();) {
         BasicLabeledDatum bld = (BasicLabeledDatum) i.next();
         Collection featureList = bld.getFeatures();
         for (Iterator j = featureList.iterator(); j.hasNext();) {
            String feature = (String) j.next();
            if (featureCounter.containsKey(feature) == false) {
               j.remove();
            }
         }
      }
      return labeledData;
   }


   private static String makeFileName(boolean SR, boolean leftToRight, int maxLength,
         int cutoff, int maxFile, boolean chPhase) {
      StringBuffer sb = new StringBuffer("cl");
      if (SR) {
         sb.append("SR");
      } else {
         sb.append("BC");
      }
      if (chPhase) {
         sb.append("CH");
      } else {
         sb.append("AT");
      }
      if (leftToRight) {
         sb.append("LR");
      } else {
         sb.append("RL");
      }
      sb.append("ml").append(maxLength);
      sb.append("co").append(cutoff).append("es").append(maxFile).append(".cl");
      return sb.toString();
   }


   static List getTrainingData(Sentence s, boolean SR, boolean leftToRight,
         boolean chunkPhase) {
      // static List getTrainingData(Sentence s, boolean SR){
      TreeBankAnnotation tba = (TreeBankAnnotation) s
            .getAnnotation(SentenceAnnotation.PENNTREEBANK);
      Tree t = tba.getNormalizedTree();
      if (!leftToRight) {
         t.mirror();
         s = s.reverse();
      }
      if (twoPhaseParsing == false) {
         List derivation = makeDerivation(t);
         return (new MaxEntDerivation(s, leftToRight))
               .getTrainingData(derivation, SR);
         // return (new MaxEntDerivation(s)).getTrainingData(derivation, SR);
      } else {
         List derivation = makeChunkDerivation(t);
         MaxEntDerivation med = new MaxEntDerivation(s, leftToRight);
         List data = med.getTrainingData(derivation, SR);
         if (chunkPhase == true) {
            return data;
         }
         derivation = makeAttachDerivation(t);
         med.initiateAttachPhase();
         return med.getTrainingData(derivation, SR);
      }
   }


   static List makeChunkDerivation(Tree t) {
      ArrayList al = new ArrayList();
      makeChunkDerivationRecursive(t, al);
      return al;
   }


   private static void makeChunkDerivationRecursive(Tree t, List al) {
      if (t.isPreTerminal()) {
         al.add(SHIFT);
         return;
      }
      for (int i = 0; i < t.getChildren().size(); i++) {
         makeChunkDerivationRecursive((Tree) t.getChildren().get(i), al);
         if (t.allChildrenPreTerminals()) {
            if (i == 0) {
               addInternedFeature(al, BEGIN + t.getLabel());
            } else {
               addInternedFeature(al, CONT + t.getLabel());
            }
         } else if (((Tree) t.getChildren().get(i)).isPreTerminal()) {
            al.add(NOOP);
         }
      }
      if (t.allChildrenPreTerminals()) {
         al.add(REDUCE);
         al.add(NOOP);
      }

   }


   private static void makeAttachDerivationRecursive(Tree t, List al) {
      if (t.isPreTerminal() || t.allChildrenPreTerminals()) {
         al.add(SHIFT);
         return;
      }
      for (int i = 0; i < t.getChildren().size(); i++) {
         makeAttachDerivationRecursive((Tree) t.getChildren().get(i), al);
         if (i == 0) {
            addInternedFeature(al, BEGIN + t.getLabel());
         } else {
            addInternedFeature(al, CONT + t.getLabel());
         }
      }
      al.add(REDUCE);
   }


   static List makeAttachDerivation(Tree t) {
      ArrayList al = new ArrayList();
      makeAttachDerivationRecursive(t, al);
      return al;
   }


   static List makeDerivation(Tree t) {
      ArrayList al = new ArrayList();
      makeDerivationRecursive(t, al);
      return al;
   }


   private static void makeDerivationRecursive(Tree t, List al) {
      if (t.isPreTerminal()) {
         al.add(SHIFT);
         return;
      }
      for (int i = 0; i < t.getChildren().size(); i++) {
         makeDerivationRecursive((Tree) t.getChildren().get(i), al);
         if (i == 0) {
            addInternedFeature(al, BEGIN + t.getLabel());
         } else {
            addInternedFeature(al, CONT + t.getLabel());
         }
      }
      al.add(REDUCE);
   }


   // path, SR, LR, cutoff, maxlength
   public static void main(String[] args) throws IOException {
      String basePath = ".";
      basePath = args[0];
      boolean SR = false;
      if (args[1].equals("SR")) {
         SR = true;
      }
      boolean LR = true;
      if (!args[2].equals("LR")) {
         LR = false;
         HeadedTreePrefs.leftToRight = false;
      }
      int CO = Integer.parseInt(args[3]);
      int ML = Integer.parseInt(args[4]);
      int ES = Integer.parseInt(args[5]);
      if (args[6].equals("CH")) {
         chunkPhase = true;
      }
      trainClassifier(SR, 200, ES, basePath, LR, CO, ML);
   }

}
