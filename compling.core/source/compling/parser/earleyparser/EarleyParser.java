package compling.parser.earleyparser;

import java.util.*;
import compling.parser.*;
import compling.utterance.*;
import compling.util.PriorityQueue;
import compling.util.StringUtilities;
import compling.grammar.*;

public class EarleyParser<PARSEKIND extends Parse, SYM, KEY, RULE extends Rule<SYM>, STATEKIND extends State<SYM, KEY, RULE, PARSEKIND, STATEKIND>>
			implements ChartParser<PARSEKIND, STATEKIND> {

	private Grammar<SYM, RULE> grammar;
	private static final String GAMMA = "GAMMA";
	private List<STATEKIND> starterStates;

	public EarleyParser(Grammar<SYM, RULE> grammar, List<STATEKIND> starterStates) {
		this.grammar = grammar;
		this.starterStates = starterStates;
	}

	public PARSEKIND getBestParse(Utterance<Word, String> utterance) {
		Chart<STATEKIND> chart = getChart(utterance);
		if (chart.getSpanningStates().hasNext()) {
			return chart.getSpanningStates().next().makeBestParse();
		}
		else {
			return null;
		}
	}

	public Chart<STATEKIND> getChart(Utterance<Word, String> utterance) {
		return getChart(utterance, starterStates);
	}

	public Chart<STATEKIND> getChart(Utterance<Word, String> utterance, List<STATEKIND> startStates) {
		EarleyChart chart = new EarleyChart(utterance.size(), startStates);
		for (int i = 0; i < utterance.size() + 1; i++) {
			// if (i < utterance.size())
			// {System.out.println("Processing Next Word: "+utterance.getElement(i));}
			PriorityQueue<STATEKIND> states = chart.getSortedStatesAt(i);
			while (states.hasNext()) {
				STATEKIND state = states.next();
				// System.out.println("Next State: "+state.getKey());
				// System.out.println(state);
				if (i < utterance.size() && !state.completed()) {
					// System.out.println(state.getNextSymbols());
					for (SYM sym : state.getNextSymbols()) {
						// System.out.println("sym: "+sym+" "+StringUtilities.addQuotes(utterance.getElement(state.getEnd()).getOrthography()));
						if (sym.equals(StringUtilities.addQuotes(utterance.getElement(state.getEnd()).getOrthography()))) {
							scannerAction(state, chart, utterance);
						}
						else {
							predictorAction(sym, state, chart);
						}

					}
				}
				else if (state.completed() && !state.isGoalState(utterance.size())
							&& chart.getStatesThatNeed(state.getLHS(), state.getStart()) != null) {
					// System.out.println("in completer branch: "+state.getKey());
					for (STATEKIND needingIt : chart.getStatesThatNeed(state.getLHS(), state.getStart())) {
						for (SYM sym : needingIt.getNextSymbols()) {
							if (sym.equals(state.getLHS())) {
								completerAction(needingIt, state, chart);
							}
						}
					}
				}
			}
		}
		return chart;
	}

	protected void predictorAction(SYM sym, STATEKIND state, EarleyChart chart) {
		// for (RULE rule : grammar.getRules(sym)) {
		// chart.enqueue(state.generateNextState(rule));
		for (STATEKIND genState : state.generateNextStates(sym)) {
			// System.out.println(sym + " : " + genState.getKey());
			chart.enqueue(genState);
		}
	}

	protected void scannerAction(STATEKIND state, EarleyChart chart, Utterance<Word, String> utterance) {
		for (STATEKIND advancedState : state.advance(StringUtilities.addQuotes(utterance.getElement(state.getEnd())
					.getOrthography()))) {
			chart.enqueue(advancedState);
		}
	}

	protected void completerAction(STATEKIND needingIt, STATEKIND state, EarleyChart chart) {
		for (STATEKIND advancedState : needingIt.advance(state)) {
			chart.enqueue(advancedState);
		}
	}

	private class EarleyChart implements Chart<STATEKIND> {

		int length;
		int numStates = 0;
		List<PriorityQueue<STATEKIND>> sortedStates = new ArrayList<PriorityQueue<STATEKIND>>();
		List<List<STATEKIND>> states = new ArrayList<List<STATEKIND>>();
		List<HashMap<SYM, List<STATEKIND>>> needs = new ArrayList<HashMap<SYM, List<STATEKIND>>>();
		HashMap<KEY, STATEKIND> present = new HashMap<KEY, STATEKIND>();
		PriorityQueue<STATEKIND> completeAnalyses = new PriorityQueue<STATEKIND>();

		private EarleyChart(int length, List<STATEKIND> starterStates) {
			this(length);
			for (STATEKIND state : starterStates) {
				this.enqueue(state);
			}
		}

		private EarleyChart(int length) {
			this.length = length;
			for (int i = 0; i < length + 1; i++) {
				sortedStates.add(new PriorityQueue<STATEKIND>());
				states.add(new ArrayList<STATEKIND>());
				needs.add(new HashMap<SYM, List<STATEKIND>>());
			}
		}

		protected PriorityQueue<STATEKIND> getSortedStatesAt(int position) {
			return sortedStates.get(position);
		}

		public List<STATEKIND> getStatesAt(int position) {
			return states.get(position);
		}

		public List<STATEKIND> getStatesThatNeed(SYM sym, int position) {
			return needs.get(position).get(sym);
		}

		public int getTotalStates() {
			return numStates;
		}

		public int getLength() {
			return length;
		}

		private void enqueue(STATEKIND chartState) {
			if (present.containsKey(chartState.getKey())) {
				present.get(chartState.getKey()).incorporate(chartState);
				return;
			}
			numStates++;
			sortedStates.get(chartState.getEnd()).add(chartState, chartState.forwardScore());
			states.get(chartState.getEnd()).add(chartState);
			present.put(chartState.getKey(), chartState);
			if (!chartState.completed()) {
				// System.out.println(chartState.getKey()+" needs:");
				for (SYM sym : chartState.getNextSymbols()) {
					// System.out.println(sym);
					List<STATEKIND> nextSym = needs.get(chartState.getEnd()).get(sym);
					if (nextSym == null) {
						nextSym = new ArrayList<STATEKIND>();
						needs.get(chartState.getEnd()).put(sym, nextSym);
					}
					nextSym.add(chartState);
				}
			}
			if (chartState.isGoalState(length)) {
				completeAnalyses.add(chartState, chartState.viterbiScore());
			}
		}

		public PriorityQueue<STATEKIND> getSpanningStates() {
			return completeAnalyses;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < length + 1; i++) {
				for (STATEKIND state : getStatesAt(i)) {
					sb.append(state.getKey()).append("\n");
				}
			}
			return sb.toString();
		}

	}
}
