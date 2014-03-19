package compling.parser;

import java.util.List;

import compling.utterance.Utterance;
import compling.utterance.Word;

public interface RobustParser<PARSEKIND> extends Parser<PARSEKIND> {

	public List<PARSEKIND> getBestPartialParse(Utterance<Word, String> utterance);

}
