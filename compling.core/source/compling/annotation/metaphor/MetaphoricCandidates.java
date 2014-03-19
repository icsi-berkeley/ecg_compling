
package compling.annotation.metaphor;

import compling.utterance.*;
import java.util.*;


public class MetaphoricCandidates implements UtteranceAnnotation {

   public static final String NOTAG = "NOTAG";
   public static final String NOMETAPHOR = "NOMETAPHOR";
   public static final String UNKNOWN = "UNKNOWN";

   private HashMap<Integer, MetaphorTag> perWordTags = new HashMap<Integer, MetaphorTag>();
   private String source = "";
   private int fileindex = -1;


   public String getSource() {
      return source;
   }

   public int getIndex() {
      return fileindex;
   }

   public MetaphoricCandidates(String source, int fileindex) {
      this.source = source;
      this.fileindex = fileindex;
   }

   public void addTag(int word, String metaphor) {
      perWordTags.put(word, new MetaphorTag(word, metaphor));
   }


   MetaphorTag getWordTag(int index) {
      return perWordTags.get(index);
   }


   Collection<MetaphorTag> getAllTags() {
      return perWordTags.values();
   }


   public String toString() {
      return "My Set:" + getAllTags().toString();
   }


   public String getAnnotationType() {
      return UtteranceAnnotation.METAPHOR;
   }

    public class MetaphorTag {
	int word;
	String metaphor;

	public MetaphorTag(int word, String metaphor) {
	    this.word = word;
	    this.metaphor = metaphor;
	}

	public int getWord() {
	    return this.word;
	}

	public String getMetaphor() {
	    return this.metaphor;
	}

	public String toString() {
	    return source + "[" + fileindex + "] word:" + word + " metaphor:" + metaphor;
	}
    }

}
