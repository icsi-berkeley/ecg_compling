
package compling.annotation.metaphor;

import java.io.*;
import java.util.*;
import compling.annotation.propbank.*;
import compling.annotation.treebank.*;
import compling.utterance.*;
import compling.util.fileutil.*;
import compling.annotation.treebank.TreeUtilities.StandardTreeNormalizer;

/**
 * This class represents a SentenceIterator that takes a PropBankIterator as its
 * argument and provides an iteration over the Sentences that have a PropBank
 * annotation and contain a target which is on the specified list.
 * 
 * JB changed this class to only look for a particular verb sense
 * 
 * @author Branimir Ciric
 */
public class MetaphorFilter implements Iterator<Sentence> {

   private PropBankIterator pbi;
   private HashSet<String> targets;
   private Sentence nextSentence = null;

   public MetaphorFilter(PropBankIterator pbi, String targetsFile) throws IOException {
      setupTargets(targetsFile);
      this.pbi = pbi;
      setupNextSentence();
   }

   private void setupTargets(String targetsFile) throws IOException {
      targets = new HashSet<String>();
      TextFileLineIterator tfli = new TextFileLineIterator(targetsFile);
      while (tfli.hasNext()) {
         String target = ((String) tfli.next()).trim().toLowerCase();
         if (!targets.contains(target)) {
            targets.add(target);
         }
      }
   }

   protected void setupNextSentence() {
      if (pbi.hasNext()) {
         Sentence s = pbi.next();
	 MetaphoricCandidates mc = null; //= new MetaphoricCandidates(s.getSource(), s.getSourceOffset());
         if (s.hasAnnotation(UtteranceAnnotation.PROPBANK)) {
            boolean hasTarget = false;
            PropBankAnnotation pba = (PropBankAnnotation) s.getAnnotation(UtteranceAnnotation.PROPBANK);
	    TreeBankAnnotation tba = (TreeBankAnnotation) s.getAnnotation(UtteranceAnnotation.PENNTREEBANK);
	    for (TargetAnnotation ta : pba.getTargetAnnotations()){
               String target = (String) ta.getTargetFrame();
               if (targets.contains(target.toLowerCase())) {
                  nextSentence = s;
                  hasTarget = true;
		  if (mc == null){mc= new MetaphoricCandidates(s.getSource(), s.getSourceOffset());}
		  mc.addTag(tba.getNormalizedIndex(ta.getTargetIndex()), MetaphoricCandidates.UNKNOWN);
               }
            }
	    if (hasTarget){
		nextSentence.addAnnotation(mc);
	    } else {
               setupNextSentence();
	    }
         } else {
            setupNextSentence();
         }
      } else {
         nextSentence = null;
      }
   }

    public Sentence next() {
       if (hasNext() == false) {throw new NoSuchElementException();}
       Sentence s = nextSentence;
       setupNextSentence();
       return s;
    }

    public boolean hasNext() {
	return nextSentence != null;
    }

    public void remove() {
       throw new UnsupportedOperationException("MetaphorFilter does not support the remove method");
    }

   public static void main(String[] args) throws IOException {
      String verbsFile = "";
      try {
	  verbsFile = args[0]; 
      } catch (Exception e) {
	  System.out.println("The arguments for this MetaphorFilter.main are:\n\t1) Verb file list\n\t");
	  System.out.println("Exception: " + e);
	  System.exit(0);
      }

      StandardTreeNormalizer strans = new StandardTreeNormalizer();
      FileFilter sff = new NumberRangeFileFilter(".mrg", 2, 5000, true);
      TreeBankFileIterator stbfit = new TreeBankFileIterator("wsj", sff, strans);
      PropBankFileIterator spbfit = new PropBankFileIterator("prop.txt", 200, 5000);
      MetaphorFilter smf = new MetaphorFilter(new PropBankIterator(stbfit, spbfit), verbsFile);
      //      System.out.println("<TXT>");
      while (smf.hasNext()) {
	  Sentence ss = smf.next();
	  for (MetaphoricCandidates.MetaphorTag mt : ((MetaphoricCandidates) ss.getAnnotation(UtteranceAnnotation.METAPHOR)).getAllTags()){
	      System.out.println(ss.getSource()+":"+ss.getSourceOffset()+" "+
				 getMarkedSentence(ss.getOrths(), mt.getWord())+"\n");
	  }
      }
      //System.out.println("</TXT>");
   }

 
   private static String getMarkedSentence(List<String> words, int index) {
      StringBuffer ms = new StringBuffer();
      int i = 0;
      //ms.append("<S>\n");
      for(String word : words){
	  if (i == index){ms.append("{").append(word).append("} ");}
	  else { 
	      ms.append(word).append(" ");
	      }
	      i++;
      }
      //ms.append("\n</S>\n\n");
      ms.append("\n\n");
      return ms.toString().trim();
   }
}
