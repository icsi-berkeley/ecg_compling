package compling.search;

import compling.util.*;

/**
 * This is an abstract interface to describe different types of search.
 * Hopefully this will make switching between search strategies simpler.
 * 
 * @author John Bryant
 * 
 */

public abstract class SearchStrategy {

	protected PriorityQueue<SearchState> goalStates;
	protected int numToFind;

	abstract protected boolean completed();

	abstract protected boolean noContinuations();

	abstract protected void nextIteration();

	abstract protected int statesExpanded();

	public PriorityQueue<SearchState> runToCompletion() {
		return runToCompletion(java.lang.Integer.MAX_VALUE, java.lang.Integer.MAX_VALUE);
	}

	public PriorityQueue<SearchState> runToCompletion(int maxIterations, int maxStates) {
		int i = 0;
		while (completed() == false) {
			if (noContinuations() == true || i > maxIterations || statesExpanded() > maxStates) {
				break;
			}
			nextIteration();
			i++;
		}
		return goalStates;
	}
}
