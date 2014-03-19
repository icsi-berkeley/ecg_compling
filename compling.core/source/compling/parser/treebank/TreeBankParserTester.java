
package compling.parser.treebank;

import compling.utterance.*;
import java.util.*;
import compling.annotation.treebank.TreeUtilities.*;
import compling.annotation.treebank.*;
import compling.grammar.cfg.*;
import compling.util.fileutil.*;
import java.io.FileFilter;
import java.io.IOException;


public class TreeBankParserTester {

   public static void testParser(List<Sentence> sentences, TreeBankParser parser) {
      long totalMillis = 0;
      EnglishPennTreebankParseEvaluator.LabeledConstituentEval eval = new EnglishPennTreebankParseEvaluator.LabeledConstituentEval(
            Collections.singleton("ROOT"), new HashSet<String>(Arrays.asList(new String[] {
                  "''", "``", ".", ":", "," })));
      int j = 0;
      for (Sentence s : sentences) {
         System.out.println(s);
         long before = System.currentTimeMillis();
         Tree guessedTree = parser.getBestParse(s);
         long after = System.currentTimeMillis();
         Tree testTree = ((TreeBankAnnotation) s.getAnnotation(UtteranceAnnotation.PENNTREEBANK)).getNormalizedTree();
         if (guessedTree == null) {
            // System.out.println("Gold:\n"+
            // TreeUtilities.PennTreeRenderer.render(testTree));
            System.out.println("NULL GUESS TREE!");
         } else {
            // System.out.println("Gold:\n"+
            // TreeUtilities.PennTreeRenderer.render(testTree));
            // System.out.println("Guess:\n"+TreeUtilities.PennTreeRenderer.render(guessedTree));

            eval.evaluate(guessedTree, testTree);

            long diff = after - before;
            // System.out.println("Sentence: "+j+"; NumWord: "+s.getLength()+";
            // NumMilliSeconds: "+diff);
            totalMillis = totalMillis + diff;
         }
         j++;
      }
      eval.display(true);
      System.out.println("Total number of trees: " + j + "  Total time: "
            + totalMillis + "  Average time: " + totalMillis / j);

   }


   public static List getTestSentences(String basePath, boolean useTagFile,
         String tagFile, int startFile, int endFile, int maxLength) throws IOException {
      List<Sentence> sentences = new ArrayList<Sentence>();
      StandardTreeNormalizer trans = new StandardTreeNormalizer();
      FileFilter ff = new NumberRangeFileFilter(".mrg", startFile, endFile, true);
      SentenceTextFileIterator sfi = null;
      if (useTagFile) {
         sfi = new SentenceTextFileIterator(tagFile);
      }

      for (TreeBankFileIterator tfi = new TreeBankFileIterator(basePath, ff, trans); tfi.hasNext();) {
         Sentence s = tfi.next();
         if (useTagFile) {
            Sentence tagging = sfi.next();
            s.addAnnotation(tagging.getAnnotation(UtteranceAnnotation.POS));
         }
         if (s.size() <= maxLength) {
            sentences.add(s);
         }
      }
      return sentences;
   }

}
