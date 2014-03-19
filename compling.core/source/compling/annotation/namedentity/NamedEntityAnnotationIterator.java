// NamedEntityAnnotationIterator.java

package compling.annotation.namedentity;

import java.io.IOException;
import java.util.ArrayList;
//import java.util.zip.DataFormatException;
import java.util.StringTokenizer;

import compling.util.fileutil.TextFileLineIterator;
import compling.utterance.Sentence;
import compling.utterance.UtteranceAnnotation;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * @author Branimir Ciric
 */
public class NamedEntityAnnotationIterator implements Iterator<Sentence> {

   public static final String TEXT = "txt";
   public static final String SENTENCE = "s";
   public static final String SLASH = "/";
   public static final String L_TAG_BRACKET = "<";
   public static final String R_TAG_BRACKET = ">";
   TextFileLineIterator tfli;
   String file;
   int currentIndex = 0;
   String tagType = null;
   Sentence nextSentence = null;


   public NamedEntityAnnotationIterator(String path) throws IOException {
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
            "NamedEntityAnnotationIterator does not support the remove method");
   }


   protected void setupNextSentence() {
      String nextLine = null;
      if (tfli.hasNext()) {
         nextLine = tfli.next();

         if (nextLine.trim().equals("<txt>")) {
            nextLine = tfli.next();
         }

         if (nextLine.trim().equals("</txt>")) {
            nextLine = null;
         }

      } else {
         System.out.println("ERROR: Ill formatted file.");
      }
      if (nextLine == null) {
         nextSentence = null;
      } else {
         nextSentence = extractData(nextLine);
      }
   }


   private Sentence extractData(String line) {
      String sentence = line.replaceAll("<s>", "").replaceAll("</s>", "").trim();
      ArrayList<String> words = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(sentence);
      NamedEntityAnnotation nea = new NamedEntityAnnotation(st.countTokens());
      int wordCount = 0;
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         if (tagType == null) {
            token = setTag(token);
         }
         token = resetTag(token, nea, wordCount);
         words.add(token);
         wordCount++;
      }
      Sentence s = new Sentence(words, file, currentIndex++);
      s.addAnnotation(nea);
      return s;
   }


   private String setTag(String token) {
      int left = token.indexOf(L_TAG_BRACKET);
      int right = token.indexOf(R_TAG_BRACKET);
      if (left != -1 && right != -1) {
         tagType = token.substring(left + 1, right);
         token = token.substring(right + 1);
      }
      return token;
   }


   private String resetTag(String token, NamedEntityAnnotation nea, int count) {
      if (tagType != null) {
         nea.addTag(count, count, tagType);
         int index = token.indexOf(L_TAG_BRACKET + SLASH + tagType + R_TAG_BRACKET);
         if (index != -1) {
            tagType = null;
            token = token.substring(0, index);
         }
      }
      return token;
   }


   private String openingTag(String s) {
      return L_TAG_BRACKET + s + R_TAG_BRACKET;
   }


   private String closingTag(String s) {
      return openingTag(SLASH + s);
   }


   public static void main(String[] args) throws IOException {
      NamedEntityAnnotationIterator neai = new NamedEntityAnnotationIterator(args[0]);
      while (neai.hasNext()) {
         Sentence s = neai.next();
         UtteranceAnnotation sa = s.getAnnotation(UtteranceAnnotation.NAMEDENTITY);
         System.out.println(s);
         System.out.println("\t\t" + sa);
      }
   }
}
