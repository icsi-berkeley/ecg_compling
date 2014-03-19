
package compling.util.ngram;

import java.util.List;


interface LanguageModel {

   public double getSentenceLogProbability(List<String> s);


   public List<String> generateSentence();

}
