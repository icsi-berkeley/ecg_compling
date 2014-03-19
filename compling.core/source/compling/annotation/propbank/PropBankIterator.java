
package compling.annotation.propbank;

//import java.io.*;
import java.io.IOException;
import java.util.*;
import compling.utterance.*;
import compling.annotation.treebank.*;
import compling.util.fileutil.NumberRangeFileFilter;
//import compling.syntax.Tree;
import compling.annotation.treebank.TreeUtilities.*;


/**
 * This class links up the Trees and the propbank annotations from the two
 * iterators.
 * 
 * Each call to next() returns a Sentence annotated by the PropBankAnnotation
 * and a TreeBankAnnotation
 * 
 * Note, that the start and stop indices on the two iterators must match.
 * Otherwise, it's possible that none of the sentences will come out annotated
 * with their propbankannotations.
 * 
 * 
 * @author John Bryant
 */

public class PropBankIterator implements Iterator<Sentence> {

   private PropBankFileIterator pbfi;
   private TreeBankFileIterator tbfi;
   private PropBankAnnotation nextPBA;
   private Sentence nextSentence = null;


   public PropBankIterator(int startFile, int endFile, String treePath,
         String propPath, TreeTransformer trans) throws IOException {
      this(new TreeBankFileIterator(treePath, new NumberRangeFileFilter(".mrg",
            startFile, endFile, true), trans), new PropBankFileIterator(propPath,
            startFile, endFile));
   }


   public PropBankIterator(TreeBankFileIterator t, PropBankFileIterator p) {
      pbfi = p;
      tbfi = t;
      if (pbfi.hasNext()) {
         nextPBA = p.next();
      }
      setupNextSentence();
   }


   public Sentence next() {
      if (hasNext() == false) {
         throw new NoSuchElementException();
      }
      Sentence s = nextSentence;
      setupNextSentence();
      return s;
   }


   public boolean hasNext() {
      return nextSentence != null;
   }


   public void remove() {
      throw new UnsupportedOperationException(
            "SentenceTextFileIterator does not support the remove method");
   }


   protected void setupNextSentence() {
      if (tbfi.hasNext() == false) {
         nextSentence = null;
         if (pbfi.hasNext() || nextPBA != null) {
            System.out
                  .println("There are still propbank annotations left unattached.");
         }
         return;
      }
      nextSentence = tbfi.next();
      // System.out.println(nextSentence.getSource()+"
      // "+nextSentence.getSourceOffset());
      if (nextPBA != null && nextSentence.getSource().equals(nextPBA.getSource())
            && nextSentence.getSourceOffset() == nextPBA.getIndex()) {
         nextSentence.addAnnotation(nextPBA);
         nextPBA = null;
         if (pbfi.hasNext()) {
            nextPBA = pbfi.next();
         }
      }
   }


   public static void main(String[] args) throws IOException {
      PropBankIterator pbi = new PropBankIterator(118, 519, "wsj", "propsm.txt",
            new StandardTreeNormalizer());
      while (pbi.hasNext()) {
         Sentence s = pbi.next();
         if (s.hasAnnotation(UtteranceAnnotation.PROPBANK)) {
            System.out.println(s);
            TreeBankAnnotation tba = (TreeBankAnnotation) s
                  .getAnnotation(UtteranceAnnotation.PENNTREEBANK);
            System.out.println(TreeUtilities.PennTreeRenderer.render(tba
                  .getOriginalTree()));
            System.out.println(s.getAnnotation(UtteranceAnnotation.PROPBANK));
         }
      }
   }

}
