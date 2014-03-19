
package compling.utterance;

//import java.io.*;
import java.io.IOException;
import java.util.*;
import compling.util.fileutil.TextFileLineIterator;
import compling.annotation.pos.POSAnnotation;


/**
 * A class that takes a path to a file and then interprets each line of the file
 * as a sentence. The words and punctuation must be separated by whitespace if
 * they are to be interpreted as separate tokens. This class can also deal with
 * tagged sentence files where each word is associated with a tag. The tag must
 * immediately follow the word, with only a "/" in between the word and tag
 * (e.g. word/tag).
 * 
 * Each call to next() and nextSentence() returns a (possibly tagged) Sentence.
 * 
 * @author John Bryant
 */

public class SentenceTextFileIterator implements Iterator<Sentence> {

   TextFileLineIterator tfli;
   String file;
   int currentIndex = 0;
   Sentence nextSentence;


   public SentenceTextFileIterator(String path) throws IOException {
      file = path;
      tfli = new TextFileLineIterator(path);
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
      if (tfli.hasNext()) {
         String nextLine = tfli.next();
         nextSentence = new Sentence(extractWords(nextLine), file, currentIndex++);
         if (nextLine.indexOf("/") != -1) { // an untagged file
            nextSentence.addAnnotation(new POSAnnotation(extractTags(nextLine)));
         }
      } else {
         nextSentence = null;
      }
   }


   private List<String> extractWords(String line) {
      ArrayList<String> words = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(line);
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         int slash = token.indexOf("/");
         if (slash != -1) {
            token = token.substring(0, slash);
         }
         words.add(token);
      }
      return words;
   }


   private List<String> extractTags(String line) {
      ArrayList<String> words = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(line);
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         int slash = token.indexOf("/");
         if (slash == -1) {
            System.out.println("Bad line in tagged file:\n\t" + line);

         }
         words.add(token.substring(slash + 1, token.length()));
      }
      return words;
   }


   public static void main(String[] args) throws IOException {
      SentenceTextFileIterator sfi = new SentenceTextFileIterator(args[0]);
      while (sfi.hasNext()) {
         System.out.println(sfi.next());
      }
   }
}
