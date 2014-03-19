
package compling.parser.treebank.maxentparser;

import compling.search.*;
import compling.annotation.treebank.*;
import compling.syntax.*;
import compling.utterance.*;
import java.util.*;
import java.io.*;
import compling.util.*;
import compling.annotation.treebank.TreeUtilities.*;
import compling.util.fileutil.*;


public class MaxEntParser implements TreeBankParser {

   private MaxEntParsingModel mepm;
   private static int NUMTOFIND = 15;
   private static int BEAMWIDTH = 15;
   private static boolean LR = true;


   public MaxEntParser(MaxEntParsingModel mepm) {
      this.mepm = mepm;
   }


   public MaxEntParsingModel getModel() {
      return mepm;
   }


   /*
    * public static Tree getBestParse(Sentence s){ if (!LR){s = s.reverse();}
    * MaxEntDerivation med = new MaxEntDerivation(s, LR); //MaxEntDerivation med =
    * new MaxEntDerivation(s); List starters = new ArrayList();
    * starters.add(med); BeamSearch bs = new BeamSearch(BEAMWIDTH, NUMTOFIND,
    * starters); PriorityQueue derivations =
    * bs.runToCompletion(10*s.getLength(), java.lang.Integer.MAX_VALUE); if
    * (derivations.hasNext()){ return ((MaxEntDerivation)
    * derivations.next()).getFinalTree(LR); //return ((MaxEntDerivation)
    * derivations.next()).getFinalTree(); } else {return null;} }
    */

   public Tree getBestParse(Sentence s) {
      if (!LR) {
         s = s.reverse();
      }
      MaxEntDerivation med = new MaxEntDerivation(s, LR);
      // MaxEntDerivation med = new MaxEntDerivation(s);
      List starters = new ArrayList();
      /*
       * starters.add(med); BeamSearch bs = new BeamSearch(BEAMWIDTH, NUMTOFIND,
       * starters); PriorityQueue derivations =
       * bs.runToCompletion(4*s.getLength(), java.lang.Integer.MAX_VALUE);
       * starters = new ArrayList(); for (int i = 0; i < BEAMWIDTH; i++){ if
       * (derivations.hasNext()){ med = (MaxEntDerivation) derivations.next();
       * //System.out.println(med); med.initiateAttachPhase();
       * starters.add(med); } }
       */
      Tree t = ((TreeBankAnnotation) s.getAnnotation(SentenceAnnotation.PENNTREEBANK))
            .getNormalizedTree();
      List derivation = MaxEntParserTrainer.makeChunkDerivation(t);
      med.getTrainingData(derivation, LR);
      starters.add(med);
      BeamSearch bs = new BeamSearch(BEAMWIDTH, NUMTOFIND, starters);
      PriorityQueue derivations = bs.runToCompletion(10 * s.getLength(),
            java.lang.Integer.MAX_VALUE);
      if (derivations.hasNext()) {
         return ((MaxEntDerivation) derivations.next()).getFinalTree(LR);
         // return ((MaxEntDerivation) derivations.next()).getFinalTree();
      } else {
         return null;
      }
   }


public static void main(String[] args) throws IOException{
	String basePath = ".";
	basePath = args[0];
	try {
	    BEAMWIDTH = Integer.parseInt(args[1]);
	} catch (Exception e){
	    System.out.println("Bad Beam Width:\n "+e);
	    System.exit(0);
	}
	NUMTOFIND = BEAMWIDTH;
	String bcFile = args[2];
	String srFile = args[3];
	int maxLength = 20;
	try {
	    maxLength = Integer.parseInt(args[4]);
	} catch (Exception e){
	    System.out.println("Bad maxSentenceLength:\n "+e);
	    System.exit(0);
	}
	
	if (!args[5].equals("LR")){
	    LR = false;
	    HeadedTreePrefs.leftToRight = false;
	}
	String bcChFile = args[6];
 	String srChFile = args[7];
	System.out.println(args[0]+" "+args[1]+" "+args[2]+" "+args[3]+" "+args[4]+" "+args[5]+" "+args[6]+" "+args[7]);
	MaxEntParsingModel mepm = new MaxEntParsingModel(srFile, bcFile, srChFile, bcChFile);
	List sentences = new ArrayList();
	StandardTreeNormalizer trans = new StandardTreeNormalizer();
	FileFilter ff = new  NumberRangeFileFilter(".mrg", 2300, 2399, true);
	SentenceFileIterator sfi = new SentenceFileIterator("alltest.tagged");
	for (TreeBankFileIterator tfi = new TreeBankFileIterator(basePath, ff, trans); tfi.hasNext();){
	    Sentence s = tfi.nextSentence();
	    Sentence tagging = sfi.nextSentence();
	    s.setPOSTags(tagging.getPOSTags());
	    if (s.getLength() <= maxLength){
		sentences.add(s);
	    }
	}
	MaxEntParser mep = new MaxEntParser(mepm);
	TreeBankParserTester.testParser( 
					 , parser);
    }}
