
package compling.annotation.propbank;

import compling.utterance.UtteranceAnnotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;

import compling.grammar.cfg.*;


/**
 * This class represents a penn propbank annotation of a sentence, implementing
 * the UtteranceAnnotation interface.
 * 
 * Sentences can have more than one target of an annotation, so this class is a
 * list of annotations of a particular target.
 * 
 * @author John Bryant
 * 
 */

public class PropBankAnnotation implements UtteranceAnnotation {

   private ArrayList<TargetAnnotation> targetAnnotations = new ArrayList<TargetAnnotation>();// can
                                                                                             // be
                                                                                             // multiple
                                                                                             // per
                                                                                             // sentence
   private String source;
   private int index;


   public PropBankAnnotation(TargetAnnotation ta) {
      addTargetAnnotation(ta);
      source = ta.getSource();
      index = ta.getIndex();
   }


   public String getSource() {
      return source;
   }


   public int getIndex() {
      return index;
   }


   public String getAnnotationType() {
      return UtteranceAnnotation.PROPBANK;
   }


   public TargetAnnotation getAnnotation(String target, int index) {
      for (TargetAnnotation pba : targetAnnotations) {
         if (pba.getTarget().equals(target) == true && pba.getTargetIndex() == index) {
            return pba;
         }
      }
      return null;
   }


   static int getSourceFileNum(String f) {
      int ret = 0;
      String sint = f.substring(11, 15);
      try {
         ret = Integer.parseInt(sint);
      } catch (NumberFormatException e) {
         System.out.println(e + " in TargetAnnotation.getSourceFileNum()");
      }
      return ret;
   }


   public int getSourceFileNum() {
      return PropBankAnnotation.getSourceFileNum(getSource());
   }


   public void addTargetAnnotation(TargetAnnotation pba) {
      targetAnnotations.add(pba);
   }


   public List<String> getAnnotatedFrames() {
      List<String> frames = new ArrayList<String>();
      for (Iterator i = targetAnnotations.iterator(); i.hasNext();) {
         TargetAnnotation ta = (TargetAnnotation) i.next();
         frames.add(ta.getTarget() + "." + ta.getTargetSubFrame());
      }
      return frames;
   }


   public List<TargetAnnotation> getTargetAnnotations() {
      return targetAnnotations;
   }


   // This loses the distinction between the "," and the "*" in the annotation
   public static ArrayList<Tree> getArgNodes(TargetAnnotation pba, String arg,
         HeadedTree ht) {
      String nodeAddresses = (String) pba.getArgAddresses(arg);
      if (nodeAddresses == null) {
         return new ArrayList<Tree>();
      }
      ArrayList<Tree> nodes = new ArrayList<Tree>();
      StringTokenizer st = new StringTokenizer(nodeAddresses, "*,");
      while (st.hasMoreTokens()) {
         String next = st.nextToken();
         String[] nodeAnno = next.split(":");
         try {
            int terminal = Integer.parseInt(nodeAnno[0]);
            int height = Integer.parseInt(nodeAnno[1]);
            Tree node = ht.getNode(terminal, height);
            nodes.add(node);
         } catch (NumberFormatException e) {
            System.out.println("nodeAnno:" + nodeAnno[0] + " " + nodeAnno[1] + " "
                  + e);
         }
      }
      return nodes;
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      for (Iterator i = targetAnnotations.iterator(); i.hasNext();) {
         TargetAnnotation pba = (TargetAnnotation) i.next();
         sb.append(pba.toString()).append("\n");
      }
      return sb.toString();
   }
}
