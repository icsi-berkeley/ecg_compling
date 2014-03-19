
package compling.parser.treebank.maxentparser;

import compling.util.PriorityQueue;
import compling.search.*;
import java.util.*;


/**
 * A class that encapsulates a beam search. Parameters to the class are the
 * beamWidth, the number of end states to find (numToFind), a vector of initial
 * SearchStates (startElements).
 * 
 * @author John Bryant
 * 
 */

// An open question is whether this should also have a mechanism for
// allowing particular kinds of PriorityQueues. Perhaps the class can be
// extended for this.
public class MEBeamSearch extends SearchStrategy {

   int beamWidth;
   HashMap currentQ = new HashMap();


   public MEBeamSearch(int beamWidth, int numToFind, List startElements) {
      this.beamWidth = beamWidth;
      this.numToFind = numToFind;
      for (Iterator i = startElements.iterator(); i.hasNext();) {
         MaxEntDerivation be = (MaxEntDerivation) i.next();
         PriorityQueue pq;
         if (currentQ.get(be.getNumRdxns()) == null) {
            pq = new PriorityQueue();
            currentQ.put(be.getNumRdxns(), pq);
         } else {
            pq = (PriorityQueue) currentQ.get(be.getNumRdxns());
         }
         pq.add(be, be.getScore());
      }
      goalStates = new PriorityQueue(numToFind);
   }


   public boolean completed() {
      return goalStates.size() == numToFind;
   }


   public boolean noContinuations() {
      return currentQ.size() == 0;
   }


   protected int statesExpanded() {
      return 0;
   }


   protected void nextIteration() {
      HashMap nextQ = new HashMap();
      addContinuations(nextQ);
      currentQ = nextQ;
   }


   private void addContinuations(HashMap next) {

      // System.out.println("Next iteration");
      for (Iterator j = currentQ.values().iterator(); j.hasNext();) {
         PriorityQueue pq = (PriorityQueue) j.next();
         int k = 0;
         while (k < beamWidth && pq.hasNext()) {
            k++;
            MaxEntDerivation be = (MaxEntDerivation) pq.next();
            if (be.isGoalState()) {
               // System.out.println("YAY GOAL STATE");
               goalStates.add(be, be.getScore());
            } else {
               // System.out.println("BOO NOT A GOAL STATE");
               List becs = be.generateContinuations(beamWidth);
               for (Iterator i = becs.iterator(); i.hasNext();) {
                  MaxEntDerivation bec = (MaxEntDerivation) i.next();
                  PriorityQueue pqc;
                  if (next.get(bec.getNumRdxns()) == null) {
                     pqc = new PriorityQueue();
                     next.put(bec.getNumRdxns(), pqc);
                  } else {
                     pqc = (PriorityQueue) next.get(bec.getNumRdxns());
                  }
                  pqc.add(bec, bec.getScore());
               }
            }

         }
      }
   }
}
