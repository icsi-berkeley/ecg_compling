
package compling.search;

import java.util.*;


/**
 * SearchState is an interface for elements that we want to do a search over. It
 * has methods for the score of the state, the generation of next states
 * (generateContinuations), and checking to see if the state is a goal state.
 * 
 * @author John Bryant
 * 
 */

public interface SearchState {

   public double getScore();


   public boolean isGoalState();


   public List<SearchState> generateContinuations(int maxContinuations);

}
