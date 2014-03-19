
package compling.search;

import compling.util.PriorityQueue;
import java.util.List;
import java.util.Iterator;


/**
 * A class that encapsulates a best first search. Parameters to the class are
 * the number of goal states to find (numToFind), a vector of initial
 * SearchStates (startElements).
 * 
 * 
 * Note that this class can also be used for A* if the SearchStates that are
 * used use a admissible hueristic.
 * 
 * @author John Bryant
 * 
 */

class BestFirstSearch extends SearchStrategy {

   private PriorityQueue<SearchState> agenda;
   private int statesExpanded = 0;


   public BestFirstSearch(int numToFind, List<SearchState> startElements) {
      this.numToFind = numToFind;
      agenda = new PriorityQueue<SearchState>();
      for (SearchState ss : startElements) {
         agenda.add(ss, ss.getScore());
      }
   }


   public boolean completed() {
      return goalStates.size() == numToFind;
   }


   public boolean noContinuations() {
      return !agenda.hasNext();
   }


   protected int statesExpanded() {
      return statesExpanded;
   }


   protected void nextIteration() {
      statesExpanded++;
      SearchState ss = (SearchState) agenda.next();
      List continuations = ss.generateContinuations(java.lang.Integer.MAX_VALUE);
      for (Iterator i = continuations.iterator(); i.hasNext();) {
         SearchState c = (SearchState) i.next();
         agenda.add(c, c.getScore());
      }
   }

}
