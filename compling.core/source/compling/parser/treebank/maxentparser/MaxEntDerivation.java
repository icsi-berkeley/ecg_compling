
package compling.parser.treebank.maxentparser;

import java.util.*;
import compling.search.SearchState;
import compling.utterance.*;
import compling.annotation.treebank.*;
import compling.classifier.*;
import compling.grammar.cfg.*;
import compling.util.PriorityQueue;
import compling.util.Counter;


public class MaxEntDerivation implements SearchState {

   int nextPosition = 0;
   List analysis;
   double score = 0;
   boolean lastOpSR = false;
   static boolean chunkPhase = true;
   boolean isConsistent = true;
   int numRdxns = 0;
   String lastOp = "SH";


   public MaxEntDerivation(Sentence input, boolean LR) {
      // public MaxEntDerivation(Sentence input){
      analysis = new ArrayList();
      for (int i = 0; i < input.getLength(); i++) {
         analysis.add(new MaxEntTree(input, i, LR));
         // analysis.add(new MaxEntTree(input, i));
      }
      shift();
      chunkPhase = true;
   }


   private MaxEntDerivation(int np, double s, List a) {
      nextPosition = np;
      score = s;
      analysis = new ArrayList();
      analysis.addAll(a);
      for (int i = 0; i < np; i++) {
         analysis.set(i, ((MaxEntTree) analysis.get(i)).shallowCopy());
      }

   }


   private MaxEntDerivation copy() {
      return new MaxEntDerivation(nextPosition, score, analysis);
   }


   public void initiateAttachPhase() {
      nextPosition = 0;
      shift();
      chunkPhase = false;
   }


   public double getScore() {
      return score;
   }


   private void updateScore(double update) {
      score = score + Math.log(update);
   }


   private boolean isConsistent() {
      return isConsistent;
   }


   private int currentTop() {
      return nextPosition - 1;
   }


   private MaxEntTree getTop() {
      return (MaxEntTree) analysis.get(currentTop());
   }


   Integer getNumRdxns() {
      return new Integer(numRdxns);
   }


   private void shift() {
      nextPosition++;
      if (nextPosition > analysis.size()) {
         isConsistent = false;
      }
      lastOpSR = true;
   }


   private void reduce() {
      if (getTop().isComplete()) {
         isConsistent = false;
         return;
      }
      getTop().finish();
      lastOpSR = true;
      numRdxns++;
   }


   private void begin(String symbol) {
      if (chunkPhase && getTreeFromOffset(-2) != null
            && getTreeFromOffset(-2).isComplete() == false) {
         isConsistent = false;
         return;
      }
      ArrayList al = new ArrayList();
      al.add(getTop());
      analysis.set(currentTop(), new MaxEntTree(symbol, al));
      lastOpSR = false;
   }


   private void noop() {
      if (getTop().isComplete() && getTreeFromOffset(-2) != null
            && getTreeFromOffset(-2).isComplete() == false) {
         isConsistent = false;
      }
      lastOpSR = false;
   }


   private void cont(String symbol) {
      MaxEntTree met = (MaxEntTree) analysis.remove(currentTop());
      nextPosition--;
      if (nextPosition > 0 && getTop().isComplete() == false
            && getTop().getLabel().equals(symbol)) {
         getTop().addChild(met);
      } else {
         isConsistent = false;
      }
      lastOpSR = false;
   }


   public MaxEntTree getTreeFromOffset(int offset) {
      if (nextPosition + offset >= analysis.size() || nextPosition + offset < 0) {
         return null;
      } else {
         return (MaxEntTree) analysis.get(nextPosition + offset);
      }
   }


   public List getTrainingData(List derivation, boolean SR) {
      // System.out.println("beginning get training data");
      ArrayList labeledData = new ArrayList();
      derivation.remove(0); // Need to get rid of the first step of the
                              // derivation (a shift)
      for (Iterator i = derivation.iterator(); i.hasNext();) {
         String op = (String) i.next();
         if (SR && !lastOpSR) {
            labeledData.add(new BasicLabeledDatum(op, SRFeatureExtractor
                  .extractSRFeatures(this)));
         } else if (!SR && lastOpSR) {
            labeledData.add(new BasicLabeledDatum(op, BCFeatureExtractor
                  .extractBCFeatures(this)));
         }
         /*
          * System.out.println(op+ " , "+this); if (!lastOpSR){
          * System.out.println(SRFeatureExtractor.extractSRFeatures(this)); }
          * else if (lastOpSR){
          * System.out.println(BCFeatureExtractor.extractBCFeatures(this)); }
          */

         processOp(op, 0);
      }
      // System.out.println("ending get training data");
      return labeledData;
   }


   public Tree getFinalTree(boolean leftToRight) {
      // public Tree getFinalTree(){
      if (leftToRight) {
         return getTop();
      } else {
         return getTop().mirror();
      }
   }


   public boolean isGoalState() {
      if (chunkPhase) {
         return analysis.size() == nextPosition && lastOp.equals("NOOP");
      } else {
         return analysis.size() == 1 && getTop().getLabel().equals("ROOT")
               && ((MaxEntTree) getTop()).completedSubTree == true;
      }
   }


   public List generateContinuations(int maxContinuations) {
      List newStates = new ArrayList();
      PriorityQueue scores;
      if (lastOpSR) {
         if (chunkPhase) {
            scores = MaxEntParser.getModel().getBCChunkScores(this);
         } else {
            scores = MaxEntParser.getModel().getBCScores(this);
         }
      } else {
         if (chunkPhase) {
            scores = MaxEntParser.getModel().getSRChunkScores(this);
         } else {
            scores = MaxEntParser.getModel().getSRScores(this);
         }
      }
      double totalMass = 0;
      int i = 0;

      while (scores.hasNext()) {
         double score = scores.getPriority();
         MaxEntDerivation med = this.copy();
         String op = (String) scores.next();
         med.processOp(op, score);
         if (med.isConsistent()) {
            newStates.add(med);
            i++;
            totalMass = totalMass + score;
         }
         if (totalMass >= .95 || i >= maxContinuations) {
            break;
         }
      }

      /*
       * List consistentScores = new ArrayList(); while (scores.hasNext()){
       * double score = scores.getPriority(); MaxEntDerivation med =
       * this.copy(); String op = (String) scores.next(); med.processOp(op, 1);
       * if (med.isConsistent()) { newStates.add(med); i++; totalMass =
       * totalMass+score; consistentScores.add(new Double(score)); } if
       * (totalMass >=.95 || i >= maxContinuations){break;} } i = 0; for
       * (Iterator j = newStates.iterator(); j.hasNext();){ MaxEntDerivation med =
       * (MaxEntDerivation) j.next(); med.updateScore(((Double)
       * consistentScores.get(i)).doubleValue()/totalMass ); i++; }
       */
      return newStates;
   }


   private void processOp(String op, double score) {
      updateScore(score);
      lastOp = op;
      if (op.length() == 1) {
         if (op.equals(MaxEntParserTrainer.SHIFT)) {
            shift();
         } else if (op.equals(MaxEntParserTrainer.REDUCE)) {
            reduce();
         } else {
            System.out.println("Bad op: " + op);
         }
      } else { // it's a begin or continue
         if (op.equals(MaxEntParserTrainer.NOOP)) {
            noop();
            return;
         }
         String newSym = op.substring(1, op.length());
         op = op.substring(0, 1);
         if (op.equals(MaxEntParserTrainer.BEGIN)) {
            begin(newSym);
         } else if (op.equals(MaxEntParserTrainer.CONT)) {
            cont(newSym);
         } else {
            System.out.println("Bad op: " + op);
         }
      }
   }


   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("LastOp=").append(lastOp).append("   ");
      for (int i = 0; i < nextPosition; i++) {
         sb.append(((Tree) analysis.get(i)).getLabel()).append(" ");
      }
      sb.append("|");
      if (nextPosition < analysis.size()) {
         sb.append(((Tree) analysis.get(nextPosition)).getLabel());
      }
      sb.append("   ").append(score);
      return sb.toString();
   }
}
