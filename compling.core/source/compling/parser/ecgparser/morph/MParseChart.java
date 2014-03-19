package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class MParseChart {
	private HashMap<String, Set<MParseState>> statesHash = new HashMap<String, Set<MParseState>>();
	private List<MParseState> statesList = new ArrayList<MParseState>();

	public MParseState addState(MParseState state) {
		return addState(state, false);
	}

	/**
	 * 
	 * @param state
	 * @param forceRepeat
	 *           When true, if the state already exists in the chart, we add another reference to it in statesList so the
	 *           main analyzer loop will hit it again
	 * @return
	 */
	public MParseState addState(MParseState state, boolean forceRepeat) {
		MParseState eState = getEquivalentState(state);
		if (eState != null) {
			if (forceRepeat)
				statesList.add(eState);
			return eState;
		}
		else {
			if (!statesHash.containsKey(state.getRuleName())) {
				statesHash.put(state.getRuleName(), new HashSet<MParseState>());
			}
			statesHash.get(state.getRuleName()).add(state);
			statesList.add(state);

			return state;
		}
	}

	public MParseState removeState(MParseState state) {
		MParseState eState = getEquivalentState(state);
		if (eState == null)
			return null;
		statesHash.get(eState.getRuleName()).remove(eState);
		return eState;
	}

	public MParseState getEquivalentState(MParseState state) {
		if (!statesHash.containsKey(state.getRuleName())) {
			return null;
		}
		Set<MParseState> sameLHS = statesHash.get(state.getRuleName());
		for (MParseState ss : sameLHS) {
			if (ss.equivalentTo(state)) {
				return ss;
			}
		}
		return null;
	}

	public boolean containsEquivalentState(MParseState state) {
		return (getEquivalentState(state) != null);
	}

	public boolean hasLHS(String lhs) {
		return statesHash.containsKey(lhs);
	}

	public Set<MParseState> getByLHS(String lhs) {
		return statesHash.get(lhs);
	}

	public int numStates() {
		return statesList.size();
	}

	public MParseState getByOffset(int i) {
		return statesList.get(i);
	}

	public String toString() {
		return statesList.toString();
	}

	public String toHTMLString() {
		String s = "<ul class=\"chart\" style=\"font-family: arial; font-size: 9pt;\">";
		for (MParseState ps : statesList) {
			s += "<li>" + ps + "</li>";
		}
		s += "</ul>";
		return s;
	}
}
