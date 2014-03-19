
package compling.util.ngram;

import java.util.*;
import compling.util.*;


/**
 * A Simple NGram class for any n
 * 
 * @Author John Bryant
 * 
 * Confusing as it is, when I use sentence here I mean a list of Strings not an
 * element of the class Sentence from compling.sentence
 * 
 */

public class NGram implements LanguageModel {

   static final String STARTSYMBOL = "<S>";
   static final String STOPSYMBOL = "</S>";
   static final String UNKNOWN = "<?>";
   Counter<String> eventCounter = new Counter<String>();
   Counter<String> normalizingCounter = new Counter<String>();
   int N;
   HashSet<String> words = new HashSet<String>();


   public NGram(int n, List sentences) {
      this.N = n;
      populateEventCounter(sentences);
   }


   private void populateEventCounter(List<String> sentences) {
      for (Iterator j = sentences.iterator(); j.hasNext();) {
         List<String> sentence = (List) j.next();
         for (int i = 0; i <= sentence.size(); i++) {
            StringBuffer sb = buildHistory(sentence, i);
            normalizingCounter.incrementCount(sb.toString(), 1);
            words.add(safeGet(sentence, i));
            sb.append(safeGet(sentence, i));
            eventCounter.incrementCount(sb.toString(), 1);
         }
      }
   }


   protected static String safeGet(List sentence, int i) {
      if (i < 0) {
         return STARTSYMBOL;
      } else if (i == sentence.size()) {
         return STOPSYMBOL;
      } else
         return (String) sentence.get(i);
   }


   protected StringBuffer buildHistory(List hist, int i) {
      StringBuffer eventBuffer = new StringBuffer();
      for (int j = N - 1; j > 0; j--) {
         eventBuffer.append(safeGet(hist, i - j)).append("|");
      }
      return eventBuffer;
   }


   public double getWordProbability(List hist, String word) {
      StringBuffer eventBuffer = buildHistory(hist, hist.size());
      double normalizingCount = normalizingCounter.getCount(eventBuffer.toString());
      eventBuffer.append(word);
      double eventCount = eventCounter.getCount(eventBuffer.toString());
      return eventCount / normalizingCount;
   }


   public double getSentenceLogProbability(List s) {
      double logProb = 0;
      for (int i = 0; i < s.size(); i++) {
         logProb = logProb + Math.log(getWordProbability(s, (String) s.get(i)));
      }
      return logProb;
   }


   String generateWord(List hist) {
      double sample = Math.random();
      double sum = 0.0;
      for (Iterator i = words.iterator(); i.hasNext();) {
         String word = (String) i.next();
         sum += getWordProbability(hist, word);
         if (sum >= sample) {
            return word;
         }
      }
      return UNKNOWN;
   }


   public List<String> generateSentence() {
      List<String> sentence = new ArrayList<String>();
      String word = generateWord(sentence);
      while (!word.equals(STOPSYMBOL)) {
         sentence.add(word);
         word = generateWord(sentence);
      }
      return sentence;
   }


   public static void main(String[] args) {
      System.out.println("Unigram");
      List<List<String>> sentences = new ArrayList<List<String>>();
      List<String> s1 = new ArrayList<String>();
      s1.add("a");
      s1.add("b");
      s1.add("c");
      sentences.add(s1);
      List<String> s2 = new ArrayList<String>();
      s2.add("b");
      s2.add("c");
      s2.add("d");
      sentences.add(s2);
      NGram unigram = new NGram(1, sentences);
      List<String> s3 = new ArrayList<String>();
      s3.add("a");
      s3.add("b");
      s3.add("c");
      s3.add("d");
      for (int i = 0; i < s3.size(); i++) {
         System.out.println(s3.get(i) + ":  "
               + unigram.getWordProbability(s3, (String) s3.get(i)));
      }
      System.out.println(unigram.generateSentence());

      System.out.println("\nBigram");
      NGram bigram = new NGram(2, sentences);
      for (int i = 0; i < s3.size(); i++) {
         System.out.println(s3.get(i) + ":  "
               + bigram.getWordProbability(s3.subList(0, i), (String) s3.get(i)));
      }
      System.out.println(bigram.generateSentence());
   }

}
