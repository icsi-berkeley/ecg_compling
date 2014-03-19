
package compling.annotation.namedentity;

import java.util.List;
import java.util.ArrayList;
import compling.utterance.UtteranceAnnotation;


/**
 * This class represents a named entity annotation of a sentence, implementing
 * the UtteranceAnnotation interface
 * 
 * @author John Bryant
 * 
 */

public class NamedEntityAnnotation implements UtteranceAnnotation {

   private String[] perWordTags;
   private int nextID = 0; // Don't make static! Should not be static!
   private List<NamedEntityTag> tags = new ArrayList<NamedEntityTag>();
   public static final String NOTAG = "NOTAG";


   public String getAnnotationType() {
      return UtteranceAnnotation.NAMEDENTITY;
   }


   public NamedEntityAnnotation(int sentenceLength) {
      perWordTags = new String[sentenceLength];
      for (int i = 0; i < perWordTags.length; i++) {
         perWordTags[i] = NOTAG;
      }
   }

   public class NamedEntityTag {

      int id;
      int start;
      int end;
      String type;


      private NamedEntityTag(int i, int s, int e, String t) {
         id = i;
         start = s;
         end = e;
         type = t;
      }


      public int getID() {
         return id;
      }


      public int getStart() {
         return start;
      }


      public int getEnd() {
         return end;
      }


      public String getType() {
         return type;
      }
   }


   /**
    * The start and end here of a particular tag are inclusive!!!! All the words
    * from start to end including end have tag type
    */
   public void addTag(int start, int end, String type) {
      tags.add(new NamedEntityTag(nextID++, start, end, type));
      for (int i = start; i <= end; i++) {
         perWordTags[i] = type;
      }
   }


   public String getWordTag(int i) {
      return perWordTags[i];
   }


   List<NamedEntityTag> getAllTags() {
      return tags;
   }
}
