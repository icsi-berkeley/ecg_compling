
package compling.util.ngram;

import java.util.*;
import compling.util.*;


/**
 * A Simple GoodTuringSmoothed NGram class for any n
 * 
 * @Author John Bryant
 * 
 */

public class GTSmoothedNGram extends NGram {

   NGram backoff;
   Counter<String> nSubK;
   Counter<String> smoothedEstimate;
   double DISCOUNT = 0;
   int K = 5;


   public GTSmoothedNGram(int n, List<String> sentences, NGram backoff) {
      super(n, sentences);
      this.backoff = backoff;
      nSubK = calcNSubKCounts(eventCounter);
      smoothedEstimate = calcSmoothedEstimate(eventCounter, K, DISCOUNT, nSubK);
      if (backoff == null) {
         smoothedEstimate.incrementCount(UNKNOWN, nSubK.getCount(new Double(1.0)));
      }
   }


   static double calcCStar(Counter<Double> counter, double count, double k) {
      // this equation taken from J&M
      double cstar = (count + 1) * counter.getCount(new Double(count + 1))
            / counter.getCount(new Double(count));
      cstar = cstar - count * (k + 1.0) * counter.getCount(new Double((k + 1.0)))
            / counter.getCount(new Double(1.0));
      cstar = cstar
            / (1 - (k + 1.0) * counter.getCount(new Double((k + 1.0)))
                  / counter.getCount(new Double(1.0)));
      return cstar;
   }


   public static Counter calcNSubKCounts(Counter<String> ngramCounter) {
      Counter<Double> nsubkCounter = new Counter<Double>();
      for (String ngram : ngramCounter.keySet()) { // .iterator();
                                                   // i.hasNext();){
         double count = ngramCounter.getCount(ngram);
         nsubkCounter.incrementCount(new Double(count), 1.0);
      }
      return nsubkCounter;
   }


   public static Counter calcSmoothedEstimate(Counter<String> ngramCounter, double k,
         double discountAmount, Counter<Double> nsk) {
      Counter<String> smoothedEstimateCounter = new Counter<String>();
      for (Iterator i = ngramCounter.keySet().iterator(); i.hasNext();) {
         String ngram = (String) i.next();
         double count = ngramCounter.getCount(ngram);
         if (count <= k) {
            smoothedEstimateCounter.incrementCount(ngram, calcCStar(nsk, count, k));
         } else {
            smoothedEstimateCounter.incrementCount(ngram, count - discountAmount);
         }
      }
      return smoothedEstimateCounter;
   }


   public double getWordProbability(List hist, String word) {
      StringBuffer eventBuffer = buildHistory(hist, hist.size());
      double normalizingCount = normalizingCounter.getCount(eventBuffer.toString());
      eventBuffer.append(word);
      double eventCount = eventCounter.getCount(eventBuffer.toString());
      if (eventCount == 0 && backoff == null) {
         eventCount = smoothedEstimate.getCount("*UNKNOWN*");
      }
      double prob = 0;
      if (normalizingCount != 0) {
         prob = smoothedEstimate.getCount(eventBuffer.toString()) / normalizingCount;
      }
      if (backoff == null) {
         return prob;
      } else {
         double alpha = calculateAlpha(hist, word);
         return prob + alpha * backoff.getWordProbability(hist, word);
      }
   }


   private double calculateAlpha(List<String> history, String word) {
      return 0;
   }

}
