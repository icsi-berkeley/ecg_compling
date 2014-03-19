
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


public class MaxEntTester {

   public static boolean LR = true;
   public static boolean SR = false;


   public static void main(String[] args) throws IOException {
      int i = 0;
      EnglishPennTreebankParseEvaluator.LabeledConstituentEval eval = new EnglishPennTreebankParseEvaluator.LabeledConstituentEval(
            Collections.singleton("ROOT"), new HashSet(Arrays.asList(new String[] {
                  "''", "``", ".", ":", "," })));
      StandardTreeNormalizer trans = new StandardTreeNormalizer();
      FileFilter ff = new NumberRangeFileFilter(".mrg", 2398, 2399, true);
      for (TreeBankFileIterator tfi = new TreeBankFileIterator(args[0], ff, trans); tfi
            .hasNext();) {
         Sentence s = tfi.nextSentence();
         if (s.getLength() <= 15) {
            System.out.println(s);
            Tree testTree = ((TreeBankAnnotation) s
                  .getAnnotation(SentenceAnnotation.PENNTREEBANK))
                  .getNormalizedTree();
            System.out.println(TreeUtilities.PennTreeRenderer.render(testTree));
            List chunk = MaxEntParserTrainer.makeChunkDerivation(testTree);
            List attach = MaxEntParserTrainer.makeAttachDerivation(testTree);
            System.out.println(chunk);
            System.out.println(attach);
            MaxEntDerivation med = new MaxEntDerivation(s, LR);
            List trainingData = med.getTrainingData(chunk, SR);
            System.out.println("onto attach");
            med.initiateAttachPhase();
            trainingData = med.getTrainingData(attach, SR);
            Tree result = med.getFinalTree(LR);
            System.out.println("Guess:\n"
                  + TreeUtilities.PennTreeRenderer.render(result));
            eval.evaluate(result, testTree);
            if (i == 10) {
               break;
            }
            i++;
         }
      }
      eval.display(true);
   }
}
