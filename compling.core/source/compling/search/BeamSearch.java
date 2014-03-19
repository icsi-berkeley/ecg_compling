package compling.search;

import compling.util.PriorityQueue;
import java.util.List;

//import java.util.Iterator;

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
public class BeamSearch extends SearchStrategy {

	int beamWidth;
	PriorityQueue<SearchState> currentQ;

	public BeamSearch(int beamWidth, int numToFind, List<SearchState> startElements) {
		this.beamWidth = beamWidth;
		this.numToFind = numToFind;
		currentQ = new PriorityQueue<SearchState>(beamWidth);
		for (SearchState be : startElements) {
			currentQ.add(be, be.getScore());
		}
		goalStates = new PriorityQueue<SearchState>(numToFind);
	}

	public boolean completed() {
		return goalStates.size() == numToFind;
	}

	public boolean noContinuations() {
		return !currentQ.hasNext();
	}

	protected int statesExpanded() {
		return 0;
	}

	protected void nextIteration() {
		PriorityQueue next = new PriorityQueue();
		addContinuationsOfTopK(next);
		currentQ = next;
	}

	private void addContinuationsOfTopK(PriorityQueue<SearchState> next) {
		int k = 0;
		// System.out.println("\nNext iteration");
		while (k < beamWidth) {
			k++;
			if (currentQ.hasNext()) {
				SearchState be = currentQ.next();
				// System.out.print(be);
				if (be.isGoalState()) {
					// System.out.println(" YAY GOAL STATE");
					goalStates.add(be, be.getScore());
				}
				else {
					// System.out.println(" BOO NOT A GOAL STATE");
					for (SearchState bec : be.generateContinuations(beamWidth))
						next.add(bec, bec.getScore());
				}
			}
			else {
				break;
			}
		}
	}
}
