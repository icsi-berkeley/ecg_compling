package compling.context;

import java.util.List;

import compling.context.MiniOntology.Individual;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;

public class Resolution {
	public Resolution(SlotChain sc) {
		this.sc = sc;
	}

	public Resolution(int sourceID, SlotChain sc, List<Individual> candidates, List<Double> scores, int bestIndex,
			boolean resolved, boolean omitted) {
		this.sc = sc;
		this.candidates = candidates;
		this.scores = scores;
		this.sourceID = sourceID;
		this.bestIndex = bestIndex;
		this.resolved = resolved;
		this.omitted = omitted;
	}

	public SlotChain sc;
	public List<Individual> candidates;
	public List<Double> scores;
	public int sourceID;
	public int bestIndex = -1;
	public boolean resolved = false;
	public boolean omitted = false;
}
