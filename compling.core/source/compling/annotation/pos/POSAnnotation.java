
package compling.annotation.pos;

import java.util.List;
//import java.util.ArrayList;
import compling.utterance.UtteranceAnnotation;


/**
 * This class represents a named entity annotation of a sentence, implementing
 * the SentenceAnnotation interface
 * 
 * @author John Bryant
 * 
 */

public class POSAnnotation implements UtteranceAnnotation {

   private List<String> tags;


   public String getAnnotationType() {
      return UtteranceAnnotation.POS;
   }


   public POSAnnotation(List<String> tags) {
      this.tags = tags;
   }


   public String getWordTag(int i) {
      return tags.get(i);
   }


   List<String> getAllTags() {
      return tags;
   }
}
