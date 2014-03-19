package compling.parser;

import compling.utterance.Utterance;
import compling.utterance.Word;

//import compling.util.PriorityQueue;

public interface Parser<PARSEKIND> {

	public PARSEKIND getBestParse(Utterance<Word, String> utterance);

	// public PriorityQueue<PARSEKIND> getOrderedParses(Utterance<Word, String> utterance);

}
