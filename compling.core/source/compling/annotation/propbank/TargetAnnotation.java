
package compling.annotation.propbank;

import java.util.*;


/**
 * This class is used to parse lines of the propbank annotation file into a
 * structured form.
 * 
 * For a prose description of how the lines should be interpreted, please refer
 * to the propbank annotation guide.
 * 
 * @author John Bryant
 */

public class TargetAnnotation {

   public static final String ARG0 = "ARG0";
   public static final String ARG1 = "ARG1";
   public static final String ARG2 = "ARG2";
   public static final String ARG3 = "ARG3";
   public static final String ARG4 = "ARG4";
   public static final String ARG5 = "ARG5";

   public static final String ARGMTMP = "ARGM-TMP"; // temporal
   public static final String ARGMLOC = "ARGM-LOC"; // locative
   public static final String ARGMMNR = "ARGM-MNR"; // manner
   public static final String ARGMPNC = "ARGM-PNC"; // purpose
   public static final String ARGMCAU = "ARGM-CAU"; // cause
   public static final String ARGMEXT = "ARGM-EXT"; // extent
   public static final String ARGMDIR = "ARGM-DIR"; // direction
   public static final String ARGMREC = "ARGM-REC"; // recipient
   public static final String ARGMPRD = "ARGM-PRD"; // predication
   public static final String ARGMNEG = "ARGM-NEG"; // negation
   public static final String ARGMMOD = "ARGM-MOD"; // modal
   public static final String ARGMADV = "ARGM-ADV"; // adverb
   public static final String ARGMDIS = "ARGM-DIS"; // discourse particle
   public static final String ARGA = "ARGA"; // causer
   public static final String rel = "rel"; // this is the label for the
                                             // surface string of the verb

   public static final String INFINITIVE = "*INFINITIVE*";
   public static final String GERUND = "*GERUND*";
   public static final String PARTICIPLE = "*PARTICIPLE*";
   public static final String FINITE = "*FINITE*";

   public static final String FUTURE = "*FUTURE*";
   public static final String PAST = "*PAST*";
   public static final String PRESENT = "*PRESENT*";

   public static final String PERFECT = "*PERFECT*";
   public static final String PROGRESSIVE = "*PROGRESSIVE*";
   public static final String BOTHPERFECTANDPROGRESSIVE = "*BOTHPERFECTANDPROGRESSIVE*";

   public static final String FIRST = "*FIRST*";
   public static final String SECOND = "*SECOND*";
   public static final String THIRD = "*THIRD*";

   public static final String ACTIVE = "*ACTIVE*";
   public static final String PASSIVE = "*PASSIVE*";

   public static final String NONE = "*NONE*";

   String annotationLine;
   HashMap<String, String> argAddresses = new HashMap<String, String>(); // links
                                                                           // an
                                                                           // argname
                                                                           // to
                                                                           // the
                                                                           // string
                                                                           // describing
                                                                           // its
                                                                           // annotation
   String targetFrame;
   int targetIndex;
   String form;
   String tense;
   String aspect;
   String person;
   String voice;
   String file;
   int fileIndex;
   String argLabelString = "";


   public String getTarget() {
      return targetFrame.substring(0, targetFrame.indexOf("."));
   }

    public String getTargetFrame(){return targetFrame;}

   public String getTargetSubFrame() {
      return targetFrame
            .substring(targetFrame.indexOf(".") + 1, targetFrame.length());
   }


   public int getTargetIndex() {
      return targetIndex;
   }


   public String getVerbTense() {
      return tense;
   }


   public String getVerbVoice() {
      return voice;
   }


   public String getVerbPerson() {
      return person;
   }


   public String getVerbForm() {
      return form;
   }


   public String getVerbAspect() {
      return aspect;
   }


   public String getSource() {
      return file;
   }


   public int getIndex() {
      return fileIndex;
   }


   public String getAnnotationLine() {
      return annotationLine;
   }


   public String getArgAddresses(String arg) {
      return argAddresses.get(arg);
   }


   public String getArgLabelString() {
      return argLabelString;
   }


   public TargetAnnotation(String annotationLine) {
      this.annotationLine = annotationLine;
      StringTokenizer st = new StringTokenizer(annotationLine);
      file = st.nextToken();// file
      fileIndex = Integer.parseInt(st.nextToken());// sentence number in file
      targetIndex = Integer.parseInt(st.nextToken());
      st.nextToken(); // annotator
      targetFrame = st.nextToken(); // sense
      parseVerbFeatures(st.nextToken()); // verb features
      // args: start here
      while (st.hasMoreTokens()) {
         String argChunk = st.nextToken();
         int splitPnt = argChunk.indexOf('-');
         String nodeString = argChunk.substring(0, splitPnt);
         String argLabel = argChunk.substring(splitPnt + 1);
         argAddresses.put(argLabel, nodeString);
         argLabelString = argLabelString + argLabel + " ";
      }
   }


   public int getSourceFileNum() {
      return PropBankAnnotation.getSourceFileNum(getSource());
   }


   private void parseVerbFeatures(String featureString) {
      char f = featureString.charAt(0);
      char t = featureString.charAt(1);
      char a = featureString.charAt(2);
      char p = featureString.charAt(3);
      char v = featureString.charAt(4);

      switch (f) {
         case 'i':
            form = INFINITIVE;
            break;
         case 'g':
            form = GERUND;
            break;
         case 'p':
            form = PARTICIPLE;
            break;
         case 'v':
            form = FINITE;
            break;
         case '-':
            form = NONE;
            break;
      }
      switch (t) {
         case 'f':
            tense = FUTURE;
            break;
         case 'p':
            tense = PAST;
            break;
         case 'n':
            tense = PRESENT;
            break;
         case '-':
            tense = NONE;
            break;
      }
      switch (a) {
         case 'p':
            aspect = PERFECT;
            break;
         case 'o':
            aspect = PROGRESSIVE;
            break;
         case 'b':
            aspect = BOTHPERFECTANDPROGRESSIVE;
            break;
         case '-':
            aspect = NONE;
            break;
      }
      switch (p) {
         case '1':
            person = FIRST;
            break;
         case '2':
            person = SECOND;
            break;
         case '3':
            person = THIRD;
            break;
         case '-':
            person = NONE;
            break;
      }
      switch (v) {
         case 'a':
            voice = ACTIVE;
            break;
         case 'p':
            voice = PASSIVE;
            break;
         case '-':
            voice = NONE;
            break;
      }
   }


   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(annotationLine).append("\n").append(targetFrame).append(" ");
      sb.append(targetIndex).append(" ");
      sb.append(form).append(" ").append(tense).append(" ");
      sb.append(aspect).append(" ").append(person).append(" ").append(voice);
      sb.append(" ").append(file).append(" ").append(fileIndex).append("\n");
      for (Iterator i = argAddresses.keySet().iterator(); i.hasNext();) {
         String arg = (String) i.next();
         sb.append(arg).append(":").append(argAddresses.get(arg)).append("   ");
      }
      return sb.toString();
   }
}
