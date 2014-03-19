
package compling.annotation.treebank;

import java.io.*;
import java.util.*;
import compling.util.fileutil.NumberRangeFileFilter;
import compling.annotation.treebank.TreeUtilities.*;
import compling.annotation.pos.*;
import compling.grammar.cfg.Tree;
import compling.utterance.*;
import compling.util.fileutil.FileUtils;


/**
 * A class that takes a root path, (if it's a directory it) gathers all tree
 * files under that path that match the filter and then generates an iterator
 * that returns sentences annoated by their trees.
 * 
 * Each call to next() returns an annotated Sentence
 * 
 * @author John Bryant
 */

public class TreeBankFileIterator implements Iterator<Sentence> {

   private Iterator files;
   private Iterator currentFileIterator = null;
   private File currentFile = null;
   private int currentIndex = 0;
   private TreeUtilities.TreeTransformer treeTransformer;
   private TreeUtilities.TreeTransformer privateTreeTransformer = new StandardTreeNormalizer();
   private Sentence nextSentence = null;


   public TreeBankFileIterator(String path, int startFile, int endFile) {
      StandardTreeNormalizer trans = new StandardTreeNormalizer();
      FileFilter ff = new NumberRangeFileFilter(".mrg", startFile, endFile, true);
      List<File> files = FileUtils.getFilesUnder(path, ff);
      this.files = files.iterator();
      treeTransformer = trans;
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


   public TreeBankFileIterator(String path, FileFilter filter,
         TreeUtilities.TreeTransformer transformer) {
      List<File> files = FileUtils.getFilesUnder(path, filter);
      this.files = files.iterator();
      treeTransformer = transformer;
      setupNextSentence();
   }


   protected void setupNextSentence() {
      if (currentFileIterator == null || currentFileIterator.hasNext() == false) {
         if (files.hasNext()) {
            currentFile = (File) files.next();
            try {
               currentFileIterator = new TreeUtilities.PennTreeReader(
                     new BufferedReader(new FileReader(currentFile)));
            } catch (IOException i) {
               System.out.println("File not found in TreeBankItemIterator: " + i);
               System.exit(0);
            }
            currentIndex = 0;
         } else {
            nextSentence = null;
            return;
         }
      }

      Tree nextTree = (Tree) currentFileIterator.next();
      List<String> words = privateTreeTransformer.transformTree(nextTree).getYield();
      List<String> POS = privateTreeTransformer.transformTree(nextTree)
            .getPreTerminalYield();
      Sentence s = new Sentence(words, currentFile.getPath(), currentIndex++);
      s.addAnnotation(new POSAnnotation(POS));
      TreeBankAnnotation tba = new TreeBankAnnotation(nextTree, treeTransformer);
      s.addAnnotation(tba);
      nextSentence = s;
   }


   public static void main(String args[]) {
      int r = 0;
      StandardTreeNormalizer trans = new StandardTreeNormalizer();
      FileFilter ff = new NumberRangeFileFilter(".mrg", 200, 2199, true);
      for (TreeBankFileIterator tfi = new TreeBankFileIterator(args[0], ff, trans); tfi
            .hasNext();) {
         Sentence s = tfi.next();
         TreeBankAnnotation tba = (TreeBankAnnotation) s
               .getAnnotation(UtteranceAnnotation.PENNTREEBANK);
         //if (r % 100 == 0) {
            System.out.println("\n***SEPARATOR***\n");
            System.out.println(TreeUtilities.PennTreeRenderer.render(tba
                  .getNormalizedTree()));
	    //}
         r++;

      }
   }
}
